///////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2007 University of Texas at Austin and (C) 2005
// University of Pennsylvania and Copyright (C) 2002, 2003 University
// of Massachusetts Amherst, Department of Computer Science.
//
// This software is licensed under the terms of the Common Public
// License, Version 1.0 or (at your option) any subsequent version.
//
// The license is approved by the Open Source Initiative, and is
// available from their website at http://www.opensource.org.
///////////////////////////////////////////////////////////////////////////////

package mstparser.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.*;

import mstparser.DependencyInstance;
import mstparser.RelationalFeature;
import mstparser.ParserOptions;

/**
 * A reader for files in CoNLL format.
 * 
 * <p>
 * Created: Sat Nov 10 15:25:10 2001
 * </p>
 * 
 * @author Jason Baldridge
 * @version $Id: CONLLReader.java 137 2013-09-10 09:33:47Z wyldfire $
 * @see mstparser.io.DependencyReader
 */
public class CONLLReader extends DependencyReader {

  protected boolean discourseMode = false;
  protected ParserOptions options;
  protected static final String[] NO_FEATURES = {};
    
  public CONLLReader(boolean discourseMode, ParserOptions options) {
    this.discourseMode = discourseMode;
    this.options = options;
    System.out.println("ignoreLemmas?    " + options.ignoreLemmas);
    System.out.println("ignoreCpostTags? " + options.ignoreCposTags);
    System.out.println("ignorePosTags?   " + options.ignorePosTags);
    System.out.println("ignoreMorphology?" + options.ignoreMorphology);
  }

  @Override
  public DependencyInstance getNext() throws IOException {

    ArrayList<String[]> lineList = new ArrayList<String[]>();

    String line = inputReader.readLine();
    while (line != null && !line.equals("") && !line.startsWith("*")) {
      lineList.add(line.split("\t"));
      line = inputReader.readLine();
      // System.out.println("## "+line);
    }

    int length = lineList.size();

    if (length == 0) {
      inputReader.close();
      return null;
    }

    String[] forms = new String[length + 1];
    String[] lemmas = new String[length + 1];
    String[] cpos = new String[length + 1];
    String[] pos = new String[length + 1];
    String[][] feats = new String[length + 1][];
    String[] deprels = new String[length + 1];
    int[] heads = new int[length + 1];
    double[] confscores = confScores ? new double[length + 1] : null;

    forms[0] = "<root>";
    lemmas[0] = "<root-LEMMA>";
    cpos[0] = "<root-CPOS>";
    pos[0] = "<root-POS>";
    deprels[0] = "<no-type>";
    heads[0] = -1;
    if (confScores)
      confscores[0] = 1;

    for (int i = 0; i < length; i++) {
      String[] info = lineList.get(i);
      forms[i + 1] = normalize(info[1]);
      lemmas[i + 1] = options.ignoreLemmas ? "_" : normalize(info[2]);
      cpos[i + 1] = options.ignoreCposTags ? "_" : info[3];
      pos[i + 1] = options.ignorePosTags ? "_" : info[4];
      feats[i + 1] = options.ignoreMorphology ? NO_FEATURES : info[5].split("\\|");
      deprels[i + 1] = labeled ? info[7] : "<no-type>";
      heads[i + 1] = Integer.parseInt(info[6]);
      if (confScores)
        confscores[i + 1] = Double.parseDouble(info[10]);
    }

    feats[0] = new String[feats[1].length];
    for (int i = 0; i < feats[1].length; i++)
      feats[0][i] = "<root-feat>" + i;

    // The following stuff is for discourse and can be safely
    // ignored if you are doing sentential parsing. (In theory it
    // could be useful for sentential parsing.)
    if (discourseMode) {
      String[][] extended_feats = new String[feats[0].length][length + 1];
      for (int i = 0; i < extended_feats.length; i++) {
        for (int j = 0; j < length + 1; j++)
          extended_feats[i][j] = feats[j][i];
      }

      feats = extended_feats;
    }

    ArrayList<RelationalFeature> rfeats = new ArrayList<RelationalFeature>();

    while (line != null && !line.equals("")) {
      rfeats.add(new RelationalFeature(length, line, inputReader));
      line = inputReader.readLine();
    }

    RelationalFeature[] rfeatsList = new RelationalFeature[rfeats.size()];
    rfeats.toArray(rfeatsList);

    // End of discourse stuff.

    // Debug
    //System.out.println("Creating a new dependency instance with the following details:");
    //System.out.println("forms: " + Arrays.toString(forms));
    //System.out.println("lemmas: " + Arrays.toString(lemmas));
    //System.out.println("cpos: " + Arrays.toString(cpos));
    //System.out.println("pos: " + Arrays.toString(pos));
    //System.out.println("feats: " + Arrays.toString(feats));
    //System.out.println("deprels: " + Arrays.toString(deprels));
    //System.out.println("heads: " + Arrays.toString(heads));
    //System.out.println("rfeatsList: " + Arrays.toString(rfeatsList));
    //System.out.println("confscores: " + Arrays.toString(confscores));
    //System.out.println();
    return new DependencyInstance(forms, lemmas, cpos, pos, feats, deprels, heads, rfeatsList,
            confscores);

  }

  @Override
  protected boolean fileContainsLabels(String file) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(file));
    String line = in.readLine();
    in.close();

    if (line.trim().length() > 0)
      return true;
    else
      return false;
  }

}
