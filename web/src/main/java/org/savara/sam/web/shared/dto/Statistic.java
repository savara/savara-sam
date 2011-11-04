/**
 * 
 */
package org.savara.sam.web.shared.dto;

import java.io.Serializable;

/**
 * @author Jeff Yu
 * @date Nov 4, 2011
 */
public class Statistic implements Serializable{
	
	private static final long serialVersionUID = -4227945503873753094L;

	private int value;
	
	private String name;

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	

}
