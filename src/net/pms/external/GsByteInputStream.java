package net.pms.external;

import java.io.*;
import java.util.Arrays;

import net.pms.PMS;

public class GsByteInputStream extends ByteArrayInputStream{
	
	private ByteArrayOutputStream out;
	private int MAX_BLOCK=15;
	private byte[] zeros;
		
	public GsByteInputStream(ByteArrayOutputStream out,int expLen) {
		super(out.toByteArray());
		this.out=out;
		if(Gs.zero_fill) {
			zeros=new byte[100];
			Arrays.fill(zeros,(byte)0);
		}
	}
	
	public boolean incBuf(byte[] newBuf) {
		int old_size=this.count;
		this.buf=newBuf;
		this.count=newBuf.length;
		return old_size!=this.count;
	}
	
	private void block() {
		int i=0;
		while(i<MAX_BLOCK) {
			PMS.debug("GS input stream block "+i);
			try {
				Thread.sleep(1000); // hope we get some bytes next time
			}
			catch (Exception e) {
				; // ignore interrupts
			}
			i++;
			if(incBuf(out.toByteArray()))
				return;
		}
		// if we end up here there is not enough bytes to read 
		// but we are desperate so we try to increase the buffer
		// one last time, if it succeeds or not is irrelevant
		PMS.debug("GS max blocked reached");
		if(Gs.zero_fill)
			incBuf(zeros);
		else
			incBuf(out.toByteArray());
	}
	
	private void block(int len) {
		if(pos+len>count)
			if(!incBuf(out.toByteArray()))
				block();
	}
	
	public int read() {
		block(1);
		return super.read();
	}
	
	public int read (byte[] b, int off, int len) {
		block(len);
		return super.read(b, off, len);
	}
}

