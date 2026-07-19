package br.com.dantesrpg.model.util;

import java.util.List;

public class PartyPreset {
	private String name;
	private List<String> characterNames;
	private String andarAtual = "NULO";
	private int estadoVisualAndar = 0;

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

	public String getAndarAtual() {
		return andarAtual == null || andarAtual.isBlank() ? "NULO" : andarAtual;
	}

	public void setAndarAtual(String andarAtual) {
		this.andarAtual = andarAtual == null || andarAtual.isBlank() ? "NULO" : andarAtual;
	}

	public int getEstadoVisualAndar() {
		return estadoVisualAndar;
	}

	public void setEstadoVisualAndar(int estadoVisualAndar) {
		this.estadoVisualAndar = Math.max(0, estadoVisualAndar);
	}
}
