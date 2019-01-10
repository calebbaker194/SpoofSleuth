package mailobject;

import java.util.HashMap;

public class MailApproval {
	private Domain domain;
	private HashMap<String,Integer> countryOccurenecs;
	
	public MailApproval(String domainName, int domainId) 
	{
		setDomain(new Domain(domainId, domainName));
	}

	public HashMap<String,Integer> getCountryOccurenecs()
	{
		return countryOccurenecs;
	}

	public void setCountryOccurenecs(HashMap<String,Integer> countryOccurenecs)
	{
		this.countryOccurenecs = countryOccurenecs;
	}
	
	public int getOccurencesForCountry(String code) 
	{
		return countryOccurenecs.get(code);
	}

	public Domain getDomain()
	{
		return domain;
	}

	public void setDomain(Domain domain)
	{
		this.domain = domain;
	}

	public int getDomainId()
	{
		return domain.getDomainId();
	}
	
}
