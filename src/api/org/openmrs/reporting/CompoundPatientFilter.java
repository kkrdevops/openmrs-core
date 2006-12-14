package org.openmrs.reporting;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.PatientSetService.BooleanOperator;

public class CompoundPatientFilter extends AbstractPatientFilter implements
		PatientFilter {

	protected final Log log = LogFactory.getLog(getClass());
	
	private BooleanOperator operator;
	private List<PatientFilter> filters;
	private String description;
	
	public CompoundPatientFilter() { }
	
	public CompoundPatientFilter(BooleanOperator operator, List<PatientFilter> filters) {
		this.operator = operator;
		this.filters = filters;
	}
	
	public List<PatientFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<PatientFilter> filters) {
		this.filters = filters;
	}

	public BooleanOperator getOperator() {
		return operator;
	}

	public void setOperator(BooleanOperator operator) {
		this.operator = operator;
	}

	public PatientSet filter(PatientSet input) {
		if (operator == BooleanOperator.AND) {
			PatientSet temp = input;
			for (PatientFilter pf : filters) {
				temp = pf.filter(temp);
			}
			return temp;
		} else {
			Set<Integer> ptIds = new HashSet<Integer>();
			for (PatientFilter pf : filters) {
				ptIds.addAll(pf.filter(input).getPatientIds());
				log.debug("or " + pf.getName() + " (" + pf.toString() + ")");
			}
			PatientSet ps = new PatientSet();
			ps.copyPatientIds(ptIds);
			return ps;
		}
	}

	public PatientSet filterInverse(PatientSet input) {
		throw new UnsupportedOperationException();
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		if (description != null)
			return description;
		else {
			StringBuilder ret = new StringBuilder();
			for (Iterator<PatientFilter> i = filters.iterator(); i.hasNext(); ) {
				PatientFilter pf = i.next();
				ret.append("[").append(pf.getName() == null ? pf.getDescription() : pf.getName()).append("]");
				if (i.hasNext())
					ret.append(" " + operator + " ");
			}
			return ret.toString();
		}
	}

	public boolean isReadyToRun() {
		return operator != null && filters != null;
	}

}
