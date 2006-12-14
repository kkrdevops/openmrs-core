package org.openmrs.cohort;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.PatientSetService;
import org.openmrs.api.PatientSetService.BooleanOperator;
import org.openmrs.api.context.Context;
import org.openmrs.reporting.AbstractReportObject;
import org.openmrs.reporting.CompoundPatientFilter;
import org.openmrs.reporting.InversePatientFilter;
import org.openmrs.reporting.PatientFilter;
import org.openmrs.reporting.PatientSet;

public class CohortSearchHistory extends AbstractReportObject {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	public class CohortSearchHistoryItemHolder {
		private PatientFilter filter;
		private PatientSet cachedResult;
		private Date cachedResultDate;
		public CohortSearchHistoryItemHolder() { }
		public PatientSet getCachedResult() {
			return cachedResult;
		}
		public void setCachedResult(PatientSet cachedResult) {
			this.cachedResult = cachedResult;
		}
		public Date getCachedResultDate() {
			return cachedResultDate;
		}
		public void setCachedResultDate(Date cachedResultDate) {
			this.cachedResultDate = cachedResultDate;
		}
		public PatientFilter getFilter() {
			return filter;
		}
		public void setFilter(PatientFilter filter) {
			this.filter = filter;
		}
	}
	
	private List<PatientFilter> searchHistory;
	private volatile List<PatientSet> cachedResults;
	private volatile List<Date> cachedResultDates;
	
	public CohortSearchHistory() {
		super.setType("org.openmrs.cohort.CohortSearchHistory");
		super.setSubType("org.openmrs.cohort.CohortSearchHistory");
		searchHistory = new ArrayList<PatientFilter>();
		cachedResults = new ArrayList<PatientSet>();
		cachedResultDates = new ArrayList<Date>();
	}
	
	public synchronized List<CohortSearchHistoryItemHolder> getItems() {
		checkArrayLengths();
		List<CohortSearchHistoryItemHolder> ret = new ArrayList<CohortSearchHistoryItemHolder>();
		for (int i = 0; i < searchHistory.size(); ++i) {
			CohortSearchHistoryItemHolder item = new CohortSearchHistoryItemHolder();
			item.setFilter(searchHistory.get(i));
			item.setCachedResult(cachedResults.get(i));
			item.setCachedResultDate(cachedResultDates.get(i));
			ret.add(item);
		}
		return ret;
	}
	
	public List<PatientFilter> getSearchHistory() {
		return searchHistory;
	}

	public void setSearchHistory(List<PatientFilter> searchHistory) {
		this.searchHistory = searchHistory;
		cachedResults = new ArrayList<PatientSet>();
		cachedResultDates = new ArrayList<Date>();
		for (int i = 0; i < searchHistory.size(); ++i) {
			cachedResults.add(null);
			cachedResultDates.add(null);
		}
	}

	public List<Date> getCachedResultDates() {
		return cachedResultDates;
	}

	public List<PatientSet> getCachedResults() {
		return cachedResults;
	}
	
	public int size() {
		return searchHistory.size();
	}
	
	public int getSize() {
		return size();
	}
	
	public synchronized void addSearchItem(PatientFilter pf) {
		checkArrayLengths();
		searchHistory.add(pf);
		cachedResults.add(null);
		cachedResultDates.add(null);
	}
	
	public synchronized void removeSearchItem(int i) {
		checkArrayLengths();
		searchHistory.remove(i);
		cachedResults.remove(i);
		cachedResultDates.remove(i);
	}

	/**
	 * @param i
	 * @return patient set resulting from the i_th filter in the search history. (cached if possible)
	 */
	public PatientSet getPatientSet(int i) {
		return getPatientSet(i, true);
	}
	
	/**
	 * @param i
	 * @param useCache whether to use a cached result, if available
	 * @return patient set resulting from the i_th filter in the search history
	 */
    public PatientSet getPatientSet(int i, boolean useCache) {
		checkArrayLengths();
		PatientSet ret = null;
		if (useCache)
			ret = cachedResults.get(i);
		if (ret == null) {
			PatientFilter pf = searchHistory.get(i);
			PatientSet everyone = Context.getPatientSetService().getAllPatients();
			ret = pf.filter(everyone);
			cachedResults.set(i, ret);
			cachedResultDates.set(i, new Date());
		}
		return ret;
	}

    //TODO: figure out whether to return empty paitentset or all patients when history is empty
    public PatientSet getLastPatientSet() {
    	if (searchHistory.size() > 0)
    		return getPatientSet(searchHistory.size() - 1);
    	else
    		//return Context.getPatientSetService().getAllPatients();
    		return new PatientSet();
    }
    
    public PatientSet getPatientSetCombineWithAnd() {
    	Set<Integer> current = null;
    	for (int i = 0; i < searchHistory.size(); ++i) {
    		PatientSet ps = getPatientSet(i);
    		if (current == null)
    			current = new HashSet<Integer>(ps.getPatientIds());
    		else
    			current.retainAll(ps.getPatientIds());
    	}
    	if (current == null)
    		return Context.getPatientSetService().getAllPatients();
    	else {
    		List<Integer> ret = new ArrayList<Integer>(current);
    		Collections.sort(ret);
    		PatientSet ps = new PatientSet();
    		ps.setPatientIds(ret);
    		return ps;
    	}
    }
    
    public PatientSet getPatientSetCombineWithOr() {
    	Set<Integer> ret = new HashSet<Integer>();
    	for (int i = 0; i < searchHistory.size(); ++i) {
    		ret.addAll(getPatientSet(i).getPatientIds());
    	}
    	return new PatientSet().copyPatientIds(ret);
    }
	
	// Just in case someone has modified the searchHistory list directly. Maybe I should make that getter return an unmodifiable list.
	// TODO: this isn't actually good enough. Use the unmodifiable list method instead
	private synchronized void checkArrayLengths() {
		int n = searchHistory.size();
		while (cachedResults.size() > n)
			cachedResults.remove(n);
		while (cachedResultDates.size() > n)
			cachedResultDates.remove(n);
		while (cachedResults.size() < n)
			cachedResults.add(null);
		while (cachedResultDates.size() < n)
			cachedResultDates.add(null);
	}
	
	/**
	 * Currently a hack version: any list of numbers and one case-insensitive instance of the word AND or OR (e.g. "and 1 2 3")
	 * Eventually this should support something like "1 and (2 or 3)"
	 * @param description
	 * @return
	 */
	public PatientFilter createCompositionFilter(String description) {
		Set<String> andWords = new HashSet<String>();
		Set<String> orWords = new HashSet<String>();
		Set<String> notWords = new HashSet<String>();
		andWords.add("and");
		orWords.add("or");
		notWords.add("not");

		List<Object> currentLine = new ArrayList<Object>();

		try {
			StreamTokenizer st = new StreamTokenizer(new StringReader(description));
			st.ordinaryChar('(');
			st.ordinaryChar(')');
			Stack<List<Object>> stack = new Stack<List<Object>>();
			while (st.nextToken() != StreamTokenizer.TT_EOF) {
				if (st.ttype == StreamTokenizer.TT_NUMBER) {
					currentLine.add(new Integer((int) st.nval));
				} else if (st.ttype == '(') {
					stack.push(currentLine);
					currentLine = new ArrayList<Object>();
				} else if (st.ttype == ')') {
					List<Object> l = stack.pop();
					l.add(currentLine);
					currentLine = l;
				} else if (st.ttype == StreamTokenizer.TT_WORD) {
					String str = st.sval.toLowerCase();
					if (andWords.contains(str))
						currentLine.add(PatientSetService.BooleanOperator.AND);
					else if (orWords.contains(str))
						currentLine.add(PatientSetService.BooleanOperator.OR);
					else if (notWords.contains(str))
						currentLine.add(PatientSetService.BooleanOperator.NOT);
					else
						throw new IllegalArgumentException("Don't recognize " + st.sval);
				}
			}
		} catch (Exception ex) {
			log.error("Error in description string: " + description, ex);
			return null; 
		}

		if (!test(currentLine)) {
			log.error("Description string failed test: " + description);
			return null;
		}
		
		return toPatientFilter(currentLine);
	}
	
	private static boolean test(List<Object> list) {
		// if length > 2, make sure there's at least one operator
		// make sure NOT is always followed by something
		// make sure not everything is a logical operator
		// can't have two logical operators in a row (unless the second is a NOT)
		boolean anyNonOperator = false;
		boolean anyOperator = false;
		boolean lastIsNot = false;
		boolean lastIsOperator = false;
		boolean childrenOkay = true;
		for (Object o : list) {
			if (o instanceof List) {
				childrenOkay &= test((List<Object>) o);
				anyNonOperator = true;
			} else if (o instanceof BooleanOperator) {
				if (lastIsOperator && (BooleanOperator) o != BooleanOperator.NOT)
					return false;
				anyOperator = true;
			} else if (o instanceof Integer) {
				anyNonOperator = true;
			} else {
				throw new RuntimeException("Programming error! unexpected class " + o.getClass());
			}
			lastIsNot = ( (o instanceof BooleanOperator) && (((BooleanOperator) o) == BooleanOperator.NOT) );
			lastIsOperator = o instanceof BooleanOperator;
		}
		if (list.size() > 2 && !anyOperator)
			return false;
		if (lastIsNot)
			return false;
		if (!anyNonOperator)
			return false;
		return true;
	}
	
	private PatientFilter toPatientFilter(List<Object> phrase) {
		// Recursive step:
		// * if anything in this list is a list, then recurse on that
		// * if anything in this list is a number, replace it with the relevant filter from the history
		log.debug("Starting with " + phrase);
		for (ListIterator<Object> i = phrase.listIterator(); i.hasNext(); ) {
			Object o = i.next();
			if (o instanceof List)
				i.set(toPatientFilter((List<Object>) o));
			else if (o instanceof Integer)
				i.set(getSearchHistory().get((Integer) o - 1));
		}
		
		// base case. All elements are PatientFilter or BooleanOperator.
		log.debug("Base case with " + phrase);
		
		// first, replace all [..., NOT, PatientFilter, ...] with [ ..., InvertedPatientFilter, ...]
		boolean invertTheNext = false;
		for (ListIterator<Object> i = phrase.listIterator(); i.hasNext(); ) {
			Object o = i.next();
			if (o instanceof BooleanOperator) {
				if ((BooleanOperator) o == BooleanOperator.NOT) {
					i.remove();
					invertTheNext = !invertTheNext;
				} else {
					if (invertTheNext)
						throw new RuntimeException("Can't have NOT AND. Test() should have failed");
				}
			} else {
				if (invertTheNext) {
					i.set(new InversePatientFilter((PatientFilter) o));
					invertTheNext = false;
				}
			}
		}
		
		log.debug("Finished with NOTs: " + phrase);
		
		// Now all we have left are PatientFilter, AND, OR
		// eventually go with left-to-right precedence, and we can combine runs of the same operator into a single one
		//     1 AND 2 AND 3 -> AND(1, 2, 3)
		//     1 AND 2 OR 3 -> OR(AND(1, 2), 3)
		// for now a hack so we take the last operator in the run, and apply that to all filters
		//     for example 1 AND 2 OR 3 -> OR(1, 2, 3)
		if (phrase.size() == 1) {
			return (PatientFilter) phrase.get(0);
		}
		BooleanOperator bo = BooleanOperator.AND;
		List<PatientFilter> args = new ArrayList<PatientFilter>();
		for (Object o : phrase)
			if (o instanceof BooleanOperator)
				bo = (BooleanOperator) o;
			else
				args.add((PatientFilter) o);
		
		return new CompoundPatientFilter(bo, args);
	}
	
}
