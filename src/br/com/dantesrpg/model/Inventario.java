package br.com.dantesrpg.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Inventario {

	private Map<String, Integer> itens;
	private Map<String, Integer> overclockData;

	// Valores para referência
	public static final int PRATA_VALE_BRONZE = 1000;
	public static final int OURO_VALE_BRONZE = 1000000;

	private int moedasBronze;
	private int moedasPrata;
	private int moedasOuro;

	public Inventario() {
		this.itens = new HashMap<>();
		this.overclockData = new HashMap<>();
		this.moedasBronze = 0;
		this.moedasPrata = 0;
		this.moedasOuro = 0;
	}

	public void adicionarItem(Item item) {
		garantirEstruturas();
		String tipo = item.getTipo();
		int quantidadeAtual = this.itens.getOrDefault(tipo, 0);
		this.itens.put(tipo, quantidadeAtual + 1);

		// Preserva overclock do item
		if (item.getGrauOverclock() > 0) {
			overclockData.put(tipo, item.getGrauOverclock());
		}
	}

	public void removerItem(Item item) {
		if (item == null) {
			return;
		}
		removerItemPorTipo(item.getTipo());
	}

	public boolean removerItemPorTipo(String tipo) {
		garantirEstruturas();
		if (tipo == null) {
			return false;
		}
		int quantidadeAtual = this.itens.getOrDefault(tipo, 0);

		if (quantidadeAtual > 1) {
			this.itens.put(tipo, quantidadeAtual - 1);
			return true;
		} else if (quantidadeAtual == 1) {
			this.itens.remove(tipo);
			overclockData.remove(tipo);
			return true;
		}
		return false;
	}

	public boolean possuiItem(String tipo) {
		garantirEstruturas();
		return tipo != null && this.itens.getOrDefault(tipo, 0) > 0;
	}

	public int getOverclockDoItem(String tipo) {
		garantirEstruturas();
		return overclockData.getOrDefault(tipo, 0);
	}

	public Map<String, Integer> getOverclockData() {
		garantirEstruturas();
		return Collections.unmodifiableMap(overclockData);
	}

	public void setOverclockData(Map<String, Integer> data) {
		this.overclockData = data == null ? new HashMap<>() : new HashMap<>(data);
	}

	public Map<String, Integer> getItensAgrupados() {
		garantirEstruturas();
		return Collections.unmodifiableMap(this.itens);
	}

	private void garantirEstruturas() {
		if (this.itens == null) {
			this.itens = new HashMap<>();
		}
		if (this.overclockData == null) {
			this.overclockData = new HashMap<>();
		}
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

	public int getPesoTotalMoedas() {
		return moedasBronze + (moedasPrata * 2) + (moedasOuro * 5);
	}

	public boolean gastarMoedasPorPeso(int pesoAlvo) {
		int[] combinacao = encontrarCombinacaoMoedasPorPeso(pesoAlvo);
		if (combinacao == null) {
			return false;
		}
		moedasOuro -= combinacao[0];
		moedasPrata -= combinacao[1];
		moedasBronze -= combinacao[2];
		return true;
	}

	public boolean podeGastarMoedasPorPeso(int pesoAlvo) {
		return encontrarCombinacaoMoedasPorPeso(pesoAlvo) != null;
	}

	private int[] encontrarCombinacaoMoedasPorPeso(int pesoAlvo) {
		if (pesoAlvo <= 0 || pesoAlvo > getPesoTotalMoedas()) {
			return null;
		}

		for (int ouro = Math.min(moedasOuro, pesoAlvo / 5); ouro >= 0; ouro--) {
			int restanteAposOuro = pesoAlvo - (ouro * 5);
			for (int prata = Math.min(moedasPrata, restanteAposOuro / 2); prata >= 0; prata--) {
				int bronze = restanteAposOuro - (prata * 2);
				if (bronze <= moedasBronze) {
					return new int[] { ouro, prata, bronze };
				}
			}
		}
		return null;
	}
}
