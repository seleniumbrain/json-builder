package org.json.builder;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Response{
	private Address address;
	private String name;
	private boolean isActive;
	private int age;
	private List<PhoneNumbersItem> phoneNumbers;
	private String email;
}