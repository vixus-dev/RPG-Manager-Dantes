package br.com.dantesrpg.model.util;

import java.util.List;

public class PartyPreset {
	private String name;
	private List<String> characterNames;

	public PartyPreset() {
	}

	public PartyPreset(String name, List<String> characterNames) {
		this.name = name;
		this.characterNames = characterNames;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getCharacterNames() {
		return characterNames;
	}

	public void setCharacterNames(List<String> characterNames) {
		this.characterNames = characterNames;
	}
}
