/*************************************************************************
*                                                                        *
*  This file is part of the 20n/act project.                             *
*  20n/act enables DNA prediction for synthetic biology/bioengineering.  *
*  Copyright (C) 2017 20n Labs, Inc.                                     *
*                                                                        *
*  Please direct all queries to act@20n.com.                             *
*                                                                        *
*  This program is free software: you can redistribute it and/or modify  *
*  it under the terms of the GNU General Public License as published by  *
*  the Free Software Foundation, either version 3 of the License, or     *
*  (at your option) any later version.                                   *
*                                                                        *
*  This program is distributed in the hope that it will be useful,       *
*  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
*  GNU General Public License for more details.                          *
*                                                                        *
*  You should have received a copy of the GNU General Public License     *
*  along with this program.  If not, see <http://www.gnu.org/licenses/>. *
*                                                                        *
*************************************************************************/

package com.act.lcms.db.analysis;

import com.act.lcms.XZ;
import com.act.lcms.db.model.StandardWell;
import com.act.lcms.plotter.WriteAndPlotMS1Results;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChemicalToMapOfMetlinIonsToIntensityTimeValues {

  private static final String FMT = "pdf";
  private static final Logger LOGGER = LogManager.getFormatterLogger(ChemicalToMapOfMetlinIonsToIntensityTimeValues.class);
  private Map<String, Map<String, List<XZ>>> peakData;

  protected ChemicalToMapOfMetlinIonsToIntensityTimeValues() {
    this.peakData = new HashMap<>();
  }

  public Set<String> getIonList() {
    return this.peakData.keySet();
  }

  public Map<String, List<XZ>> getMetlinIonsOfChemical(String chemical) {
    return this.peakData.get(chemical);
  }

  public void addIonIntensityTimeValueToChemical(String chemical, String ion, List<XZ> intensityAndTimeValues) {
    Map<String, List<XZ>> val = this.peakData.get(chemical);
    if (val == null) {
      val = new HashMap<>();
    }
    val.put(ion, intensityAndTimeValues);
    this.peakData.put(chemical, val);
  }

  private static Double findPeakMaxIntensity(List<XZ> intensityTimeValues) {
    Double maxIntensity = 0.0d;
    for (XZ val : intensityTimeValues) {
      maxIntensity = Math.max(maxIntensity, val.getIntensity());
    }
    return maxIntensity;
  }

  /**
   * This function plots the positive ion and negative control ions for a given metlin ion mass per plot.
   * @param searchMz - The mz value which is used for finding spectra.
   * @param plottingDirectory - The directory where the plots will live.
   * @param positiveChemical - The positive chemical is used to make sure it is placed at the top of the spectra plot.
   * @return This function returns a map of ion to absolute paths where the plot lives.
   * @throws IOException
   */
  public Map<String, String> plotPositiveAndNegativeControlsForEachMetlinIon(
      Pair<String, Double> searchMz,
      String plottingDirectory,
      String positiveChemical,
      List<StandardWell> standardWells)
      throws IOException {
    Map<String, String> ionToPlottingFilePath = new HashMap<>();
    Map<String, Double> individualMaxIntensities = new HashMap<>();
    WriteAndPlotMS1Results plottingUtil = new WriteAndPlotMS1Results();

    //rearrange the order of plotting
    ArrayList<String> orderedPlotChemicalTitles = new ArrayList<>(this.peakData.keySet().size());
    for (String chemical : peakData.keySet()) {
      if (chemical.equals(positiveChemical)) {
        orderedPlotChemicalTitles.add(0, chemical);
      } else {
        orderedPlotChemicalTitles.add(chemical);
      }
    }

    // This variable is used as a part of the file path dir to uniquely identify the pos/neg wells for the chemical.
    StringBuilder indexedPath = new StringBuilder();
    for (StandardWell well : standardWells) {
      indexedPath.append(Integer.toString(well.getId()) + "-");
    }

    for (String ion : this.peakData.get(searchMz.getLeft()).keySet()) {
      LinkedHashMap<String, List<XZ>> ms1s = new LinkedHashMap<>();
      Map<String, Double> metlinMasses = new HashMap<>();
      Double maxIntensity = 0.0d;

      for (String chemical : orderedPlotChemicalTitles) {
        List<XZ> ionValues = this.peakData.get(chemical).get(ion);
        ms1s.put(chemical, ionValues);
        Double localMaxIntensity = findPeakMaxIntensity(ionValues);
        maxIntensity = Math.max(maxIntensity, localMaxIntensity);
        individualMaxIntensities.put(chemical, localMaxIntensity);
        metlinMasses.put(chemical, searchMz.getValue());
      }

      String relativePath = searchMz.getLeft() + "_" + indexedPath.toString() + "_" + ion;

      File absolutePathFileWithoutExtension = new File(plottingDirectory, relativePath);
      String absolutePathWithoutExtension = absolutePathFileWithoutExtension.getAbsolutePath();

      plottingUtil.plotSpectra(
          ms1s, maxIntensity, individualMaxIntensities, metlinMasses, absolutePathWithoutExtension, this.FMT, false, false);
      ionToPlottingFilePath.put(ion, relativePath + "." + this.FMT);
    }

    return ionToPlottingFilePath;
  }

  /**
   * This function plots a combination of positive and negative control intensity-time values.
   * @param searchMzs A list of mass charge values
   * @param plottingPath The wells used for the analysis. This variable is mainly used for
   * @param peakDataPos The postive intensity-time value
   * @param peakDataNegs The negative controls intensity-time values
   * @param plottingDirectory The directory where the plots are going to be placed in
   * @return
   * @throws IOException
   */
  public static Map<String, String> plotPositiveAndNegativeControlsForEachMZ(
      Set<Pair<String, Double>> searchMzs, String plottingPath, ChemicalToMapOfMetlinIonsToIntensityTimeValues peakDataPos,
      List<ChemicalToMapOfMetlinIonsToIntensityTimeValues> peakDataNegs, String plottingDirectory)
      throws IOException {

    Map<String, String> result = new HashMap<>();
    Map<String, Double> individualMaxIntensities = new HashMap<>();
    WriteAndPlotMS1Results plottingUtil = new WriteAndPlotMS1Results();

    for (Pair<String, Double> mz : searchMzs) {
      LinkedHashMap<String, List<XZ>> ms1s = new LinkedHashMap<>();
      Map<String, Double> metlinMasses = new HashMap<>();
      Double maxIntensity = 0.0d;

      String chemicalAndIonName = mz.getLeft();
      Double massChargeValue = mz.getRight();

      // Get positive ion results
      String positiveChemicalName = AnalysisHelper.constructChemicalAndScanTypeName(chemicalAndIonName, ScanData.KIND.POS_SAMPLE);
      List<XZ> ionValuesPos = peakDataPos.peakData.get(positiveChemicalName).get(chemicalAndIonName);
      ms1s.put(positiveChemicalName, ionValuesPos);
      Double localMaxIntensityPos = findPeakMaxIntensity(ionValuesPos);
      maxIntensity = Math.max(maxIntensity, localMaxIntensityPos);
      individualMaxIntensities.put(positiveChemicalName, localMaxIntensityPos);
      metlinMasses.put(positiveChemicalName, massChargeValue);

      // Get negative control results
      Integer negNameCounter = 0;
      for (ChemicalToMapOfMetlinIonsToIntensityTimeValues peakDataNeg : peakDataNegs) {
        String negativeChemicalName = AnalysisHelper.constructChemicalAndScanTypeName(chemicalAndIonName, ScanData.KIND.NEG_CONTROL);
        String negativeChemicalNameId = negativeChemicalName + "_" + negNameCounter.toString();
        List<XZ> ionValuesNeg = peakDataNeg.peakData.get(negativeChemicalName).get(chemicalAndIonName);
        ms1s.put(negativeChemicalNameId, ionValuesNeg);
        Double localMaxIntensityNeg = findPeakMaxIntensity(ionValuesNeg);
        maxIntensity = Math.max(maxIntensity, localMaxIntensityNeg);
        individualMaxIntensities.put(negativeChemicalNameId, localMaxIntensityNeg);
        metlinMasses.put(negativeChemicalNameId, massChargeValue);
        negNameCounter++;
      }

      String relativePath = massChargeValue.toString() + "_" + plottingPath + "_" + chemicalAndIonName;
      File absolutePathFileWithoutExtension = new File(plottingDirectory, relativePath);
      String absolutePathWithoutExtension = absolutePathFileWithoutExtension.getAbsolutePath();
      String absolutePathWithExtension = absolutePathWithoutExtension + "." + FMT;

      // Check if the plotting file already exists. If it does, we should not overwrite it. Instead, we just change
      // the path name by appending a counter till the collision no longer exists.
      // TODO: Implement an elegant solution to this problem.
      File duplicateFile = new File(absolutePathWithExtension);
      Integer fileDuplicateCounter = 0;
      while (duplicateFile.exists() && !duplicateFile.isDirectory()) {
        LOGGER.warn("Duplicate file exists for %s, writing to another file", duplicateFile.getAbsolutePath());
        fileDuplicateCounter++;
        relativePath = relativePath + "_" + fileDuplicateCounter.toString();
        absolutePathFileWithoutExtension = new File(plottingDirectory, relativePath);
        absolutePathWithoutExtension = absolutePathFileWithoutExtension.getAbsolutePath();
        absolutePathWithExtension = absolutePathWithoutExtension + "." + FMT;
        duplicateFile = new File(absolutePathWithExtension);
      }

      LOGGER.info("Wrote plot to %s", absolutePathWithoutExtension);

      plottingUtil.plotSpectra(
          ms1s, maxIntensity, individualMaxIntensities, metlinMasses, absolutePathWithoutExtension, FMT, false, false);

      result.put(mz.getLeft(), relativePath + "." + FMT);
    }

    return result;
  }
}
