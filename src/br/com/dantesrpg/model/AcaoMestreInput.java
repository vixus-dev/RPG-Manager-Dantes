package br.com.dantesrpg.model;

import java.util.HashMap;
import java.util.Map;

import br.com.dantesrpg.model.enums.ModoAtaque;

import java.util.List;

public class AcaoMestreInput {
	private Personagem ator;
	private Item itemSendoUsado;
	private List<Personagem> alvos;
	private Habilidade habilidade;
	private FantasmaNobre fantasmaNobre;
	private ModoAtaque modoAtaque = ModoAtaque.NORMAL;
	private int tirosExtras = 0;
	private String opcaoEscolhida;
	private int epicentroX = -1;
	private int epicentroY = -1;
	private Map<String, Integer> resultadosDados;

	public AcaoMestreInput(Personagem ator, List<Personagem> alvos, Habilidade habilidade) {
		this.ator = ator;
		this.alvos = alvos;
		this.habilidade = habilidade;
		this.fantasmaNobre = null;
		this.itemSendoUsado = null;
		this.resultadosDados = new HashMap<>();
	}

	// Construtor para o Fantasma nobre
	public AcaoMestreInput(Personagem ator, List<Personagem> alvos, FantasmaNobre fn) {
		this.ator = ator;
		this.alvos = alvos;
		this.habilidade = null;
		this.fantasmaNobre = fn; // Green fn
		this.itemSendoUsado = null;
		this.resultadosDados = new HashMap<>();
	}

	public void adicionarResultadoDado(String nomeDado, int resultado) {
		this.resultadosDados.put(nomeDado, resultado);
	}

	// Getters para todos os campos
	public Personagem getAtor() {
		return ator;
	}

	public List<Personagem> getAlvos() {
		return alvos;
	}

	public Habilidade getHabilidade() {
		return habilidade;
	}

	public FantasmaNobre getFantasmaNobre() {
		return fantasmaNobre;
	}

	public int getResultadoDado(String nomeDado) {
		return this.resultadosDados.getOrDefault(nomeDado, -1);
	}

	public Item getItemSendoUsado() {
		return this.itemSendoUsado;
	}

	// setters escondidinhos
	public void setItemSendoUsado(Item item) {
		this.itemSendoUsado = item;
	}

	public void setEpicentro(int x, int y) {
		this.epicentroX = x;
		this.epicentroY = y;
	}

	// mais getters
	public int getEpicentroX() {
		return epicentroX;
	}

	public int getEpicentroY() {
		return epicentroY;
	}

	public String getOpcaoEscolhida() {
		return opcaoEscolhida;
	}

	public void setOpcaoEscolhida(String op) {
		this.opcaoEscolhida = op;
	}

	public ModoAtaque getModoAtaque() {
		return modoAtaque;
	}

	public void setModoAtaque(ModoAtaque modo) {
		this.modoAtaque = modo;
	}

	public int getTirosExtras() {
		return tirosExtras;
	}

	public void setTirosExtras(int extras) {
		this.tirosExtras = extras;
	}

}