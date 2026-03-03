package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import java.util.ArrayList;
import java.util.List;

public class Grimorio extends Arma {

	private int maxSlots;
	private List<Habilidade> magiasArmazenadas;

	public Grimorio(String nome, String categoria, String descricao, Raridade raridade, int valorMoedas, int danoBase,
			Atributo atributo, int custoTU, int alcance, int maxSlots) {
		// Grimórios geralmente são mágicos, sem munição
		super(nome, categoria, descricao, raridade, valorMoedas, danoBase, 1, atributo, custoTU, alcance, null,
				"Grimorio", 0);
		this.maxSlots = maxSlots;
		this.magiasArmazenadas = new ArrayList<>();
	}

	public int getMaxSlots() {
		return maxSlots;
	}

	public List<Habilidade> getMagiasArmazenadas() {
		return magiasArmazenadas;
	}

	public boolean aprenderMagia(Habilidade magia) {
		if (magiasArmazenadas.size() < maxSlots) {
			magiasArmazenadas.add(magia);
			return true;
		}
		return false; // Grimório cheio
	}

	public void esquecerMagia(Habilidade magia) {
		magiasArmazenadas.remove(magia);
	}

	// Método auxiliar para carregar do JSON
	public void setMagiasIniciais(List<Habilidade> magias) {
		this.magiasArmazenadas.clear();
		if (magias != null) {
			for (Habilidade h : magias) {
				if (magiasArmazenadas.size() < maxSlots) {
					magiasArmazenadas.add(h);
				}
			}
		}
	}
}