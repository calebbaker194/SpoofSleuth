package mailobject;

import java.io.InputStream;

public class MailAttachment {
	private String name;
	private InputStream attachment;
	
	public MailAttachment(String name, InputStream attachment)
	{
		setName(name);
		setAttachment(attachment);
	}
	
	public MailAttachment(InputStream attachment)
	{
		setAttachment(attachment);
	}
	
	public MailAttachment()
	{
		
	}

	public InputStream getAttachment()
	{
		return attachment;
	}

	public void setAttachment(InputStream attachment)
	{
		this.attachment = attachment;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	
}
