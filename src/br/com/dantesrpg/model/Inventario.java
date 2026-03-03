package br.com.dantesrpg.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Inventario {

	private Map<String, Integer> itens;

	// Valores para referência
	public static final int PRATA_VALE_BRONZE = 100;
	public static final int OURO_VALE_BRONZE = 10000;

	private int moedasBronze;
	private int moedasPrata;
	private int moedasOuro;

	public Inventario() {
		this.itens = new HashMap<>();
		this.moedasBronze = 0;
		this.moedasPrata = 0;
		this.moedasOuro = 0;
	}

	public void adicionarItem(Item item) {
		String tipo = item.getTipo();
		int quantidadeAtual = this.itens.getOrDefault(tipo, 0);
		this.itens.put(tipo, quantidadeAtual + 1);
	}

	public void removerItem(Item item) {
		String tipo = item.getTipo();
		int quantidadeAtual = this.itens.getOrDefault(tipo, 0);

		if (quantidadeAtual > 1) {
			this.itens.put(tipo, quantidadeAtual - 1);
		} else if (quantidadeAtual == 1) {
			this.itens.remove(tipo);
		}
	}

	public Map<String, Integer> getItensAgrupados() {
		return Collections.unmodifiableMap(this.itens);
	}

	// Getters e Setters
	public int getMoedasBronze() {
		return moedasBronze;
	}

	public void setMoedasBronze(int moedasBronze) {
		this.moedasBronze = moedasBronze;
	}

	public int getMoedasPrata() {
		return moedasPrata;
	}

	public void setMoedasPrata(int moedasPrata) {
		this.moedasPrata = moedasPrata;
	}

	public int getMoedasOuro() {
		return moedasOuro;
	}

	public void setMoedasOuro(int moedasOuro) {
		this.moedasOuro = moedasOuro;
	}

	public int getValorTotalEmBronze() {
		return moedasBronze + (moedasPrata * PRATA_VALE_BRONZE) + (moedasOuro * OURO_VALE_BRONZE);
	}

	public void receber(int valorEmBronze) {
		if (valorEmBronze <= 0)
			return;
		this.moedasBronze += valorEmBronze;
	}

	public void receberPrata(int quantidade) {
		if (quantidade <= 0)
			return;
		this.moedasPrata += quantidade;
	}

	public void receberOuro(int quantidade) {
		if (quantidade <= 0)
			return;
		this.moedasOuro += quantidade;
	}

	public boolean gastarBronze(int qtd) {
		if (this.moedasBronze >= qtd) {
			this.moedasBronze -= qtd;
			return true;
		}
		return false;
	}

	public boolean gastarPrata(int qtd) {
		if (this.moedasPrata >= qtd) {
			this.moedasPrata -= qtd;
			return true;
		}
		return false;
	}

	public boolean gastarOuro(int qtd) {
		if (this.moedasOuro >= qtd) {
			this.moedasOuro -= qtd;
			return true;
		}
		return false;
	}
}