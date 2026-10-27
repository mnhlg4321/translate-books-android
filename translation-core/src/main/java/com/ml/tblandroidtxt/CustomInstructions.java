package com.ml.tblandroidtxt;

public class CustomInstructions {
    public String translation;
    public String refinement;

    public boolean hasTranslation() { return translation != null && !translation.trim().isEmpty(); }
    public boolean hasRefinement() { return refinement != null && !refinement.trim().isEmpty(); }
}
