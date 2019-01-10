package mailobject;

public class EmailInfo {
	private String returnPath;
	private String resolvedTo;
	private String deliveredTo;
	private String mailFrom;
	private String dkim;
	private String from;
	private String replyTo;
	private String headerDomain;
	private String smtpDomain;
	public String getReturnPath()
	{
		return returnPath;
	}
	public void setReturnPath(String returnPath)
	{
		this.returnPath = returnPath;
	}
	public String getResolvedTo()
	{
		return resolvedTo;
	}
	public void setResolvedTo(String resolvedTo)
	{
		this.resolvedTo = resolvedTo;
	}
	public String getDeliveredTo()
	{
		return deliveredTo;
	}
	public void setDeliveredTo(String deliveredTo)
	{
		this.deliveredTo = deliveredTo;
	}
	public String getMailFrom()
	{
		return mailFrom;
	}
	public void setMailFrom(String mailFrom)
	{
		this.mailFrom = mailFrom;
	}
	public String getDkim()
	{
		return dkim;
	}
	public void setDkim(String dkim)
	{
		this.dkim = dkim;
	}
	public String getReplyTo()
	{
		return replyTo;
	}
	public void setReplyTo(String replyTo)
	{
		this.replyTo = replyTo;
	}
	public String getFrom()
	{
		return from;
	}
	public void setFrom(String from)
	{
		this.from = from;
	}
	public String getHeaderDomain()
	{
		return headerDomain;
	}
	public void setHeaderDomain(String headerDomain)
	{
		this.headerDomain = headerDomain;
	}
	public String getSmtpDomain()
	{
		return smtpDomain;
	}
	public void setSmtpDomain(String smtpDomain)
	{
		this.smtpDomain = smtpDomain;
	}
	
	public String toString()
	{
		return "Message Info\n-----------\n"
				+ "mail From: " + getMailFrom()
				+ "\nreturn Path: " + getReturnPath()
				+ "\nresolved To: " + getResolvedTo()
				+ "\nDelivered To: " + getDeliveredTo()
				+ "\ndkim: " + getDkim()
				+ "\nfrom: " + getFrom()
				+ "\nreply to: " + getReplyTo()
				+ "\nheader Domain: " + getHeaderDomain()
				+ "\nsmtp Domaion: " + getSmtpDomain();
	}
}
