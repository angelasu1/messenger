import java.util.Date;

public class BlockedIP {
	private String address;
	private Date blockedTime;

	public BlockedIP(String addr) {
		this.address = addr;
		this.blockedTime = new Date();
	}

	public String getIPAddress() {
		return address;
	}

	public Date getBlockedTime() {
		return blockedTime;
	}
}