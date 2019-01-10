package mailobject;

public class Domain {
	private int domainId;
	private String domainName;
	private String domainCountry;
	
	public Domain(int domainId)
	{
		setDomainId(domainId);
	}
	public Domain(int domainId, String domainName)
	{
		setDomainId(domainId);
		setDomainName(domainName);
	}
	public Domain(int domainId, String domainName, String domainCountry)
	{
		setDomainId(domainId);
		setDomainName(domainName);
		setDomainCountry(domainCountry);
	}
	
	public int getDomainId()
	{
		return domainId;
	}
	public void setDomainId(int domainId)
	{
		this.domainId = domainId;
	}
	public String getDomainName()
	{
		return domainName;
	}
	public void setDomainName(String domainName)
	{
		this.domainName = domainName;
	}
	public String getDomainCountry()
	{
		return domainCountry;
	}
	public void setDomainCountry(String domainCountry)
	{
		this.domainCountry = domainCountry;
	}
}
