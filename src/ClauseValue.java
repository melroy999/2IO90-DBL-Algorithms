public class ClauseValue {
	private String cValue;
	private boolean cPositive;
	
	public ClauseValue(String value, boolean negation){
		cValue = value;
		cPositive = negation;
	}
	
	public ClauseValue negation() {
        return new ClauseValue(cValue, !this.isPositive());
    }
	
	public String getValue() {
        return cValue;
    }

    public boolean isPositive() {
        return cPositive;
    }
    
    @Override
    public String toString() {
        return (isPositive() ? "(" : "~(") + cValue + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        ClauseValue clauseObject = (ClauseValue) obj;
        return (clauseObject.isPositive() == isPositive()) && (clauseObject.getValue().equals(this.cValue));
    }
    
    @Override
    public int hashCode() {
        return (isPositive() ? 1 : 31) * cValue.hashCode();
    }
}
