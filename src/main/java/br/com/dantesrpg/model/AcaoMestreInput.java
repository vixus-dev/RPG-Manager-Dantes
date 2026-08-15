package br.com.dantesrpg.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import br.com.dantesrpg.model.enums.ModoAtaque;
import br.com.dantesrpg.model.map.CoordenadaMapa;
import br.com.dantesrpg.model.combat.OrigemRolagem;

import java.util.List;

public class AcaoMestreInput {
	public record AreaSelecionada(CoordenadaMapa epicentro, List<Personagem> alvos) {
		public AreaSelecionada {
			if (epicentro == null) {
				throw new IllegalArgumentException("O epicentro da área não pode ser nulo.");
			}
			alvos = alvos == null ? List.of() : List.copyOf(alvos);
		}
	}

	private Personagem ator;
	private Item itemSendoUsado;
	private List<Personagem> alvos;
	private Habilidade habilidade;
	private FantasmaNobre fantasmaNobre;
	private List<Arma> armasSelecionadas = new ArrayList<>();
	private ModoAtaque modoAtaque = ModoAtaque.NORMAL;
	private int tirosExtras = 0;
	/** Movimento separado para potencializar uma ação, sem confundir com rajadas. */
	private int movimentoReservado = 0;
	private String opcaoEscolhida;
	private List<AreaSelecionada> areasSelecionadas = new ArrayList<>();
	private Map<String, Integer> resultadosDados;
	private Map<String, OrigemRolagem> origensDados = new HashMap<>();
	private Boolean criticoManual = null; // null = auto, true = crit forçado, false = sem crit

	public AcaoMestreInput(Personagem ator, List<Personagem> alvos, Habilidade habilidade) {
		this.ator = ator;
		this.alvos = alvos == null ? new ArrayList<>() : new ArrayList<>(alvos);
		this.habilidade = habilidade;
		this.fantasmaNobre = null;
		this.itemSendoUsado = null;
		this.resultadosDados = new HashMap<>();
	}

	// Construtor para o Fantasma nobre
	public AcaoMestreInput(Personagem ator, List<Personagem> alvos, FantasmaNobre fn) {
		this.ator = ator;
		this.alvos = alvos == null ? new ArrayList<>() : new ArrayList<>(alvos);
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

	public List<Arma> getArmasSelecionadas() {
		if (armasSelecionadas == null || armasSelecionadas.isEmpty()) {
			return ator != null ? ator.getArmasEquipadas() : new ArrayList<>();
		}
		return armasSelecionadas;
	}

	public void setArmasSelecionadas(List<Arma> armasSelecionadas) {
		this.armasSelecionadas = armasSelecionadas != null ? new ArrayList<>(armasSelecionadas) : new ArrayList<>();
	}

	public int getResultadoDado(String nomeDado) {
		return this.resultadosDados.getOrDefault(nomeDado, -1);
	}

	public Map<String, Integer> getResultadosDados() {
		return Map.copyOf(this.resultadosDados);
	}

	public void definirOrigemResultadoDado(String nomeDado, OrigemRolagem origem) {
		if (nomeDado != null && origem != null) {
			origensDados.put(nomeDado, origem);
		}
	}

	public Map<String, OrigemRolagem> getOrigensDados() {
		return Map.copyOf(origensDados);
	}

	public Item getItemSendoUsado() {
		return this.itemSendoUsado;
	}

	// setters escondidinhos
	public void setItemSendoUsado(Item item) {
		this.itemSendoUsado = item;
	}

	public void setEpicentro(int x, int y) {
		this.areasSelecionadas = new ArrayList<>(
				List.of(new AreaSelecionada(new CoordenadaMapa(x, y), this.alvos)));
	}

	// mais getters
	public int getEpicentroX() {
		return areasSelecionadas.isEmpty() ? -1 : areasSelecionadas.get(0).epicentro().x();
	}

	public int getEpicentroY() {
		return areasSelecionadas.isEmpty() ? -1 : areasSelecionadas.get(0).epicentro().y();
	}

	public List<AreaSelecionada> getAreasSelecionadas() {
		return List.copyOf(areasSelecionadas);
	}

	public List<CoordenadaMapa> getEpicentros() {
		return areasSelecionadas.stream().map(AreaSelecionada::epicentro).toList();
	}

	public void setAreasSelecionadas(List<AreaSelecionada> areas) {
		this.areasSelecionadas = new ArrayList<>();
		if (areas != null) {
			for (AreaSelecionada area : areas) {
				if (area == null) {
					throw new IllegalArgumentException("A lista de áreas selecionadas não pode conter valores nulos.");
				}
				this.areasSelecionadas.add(area);
			}
		}
		this.alvos.clear();
		for (AreaSelecionada area : this.areasSelecionadas) {
			this.alvos.addAll(area.alvos());
		}
	}

	/**
	 * Retorna o índice humano (1..N) da área que originou o impacto na lista
	 * achatada de alvos. Retorna -1 quando a ação não possui áreas estruturadas.
	 */
	public int getNumeroAreaDoImpacto(int indiceImpacto) {
		if (indiceImpacto < 0) {
			return -1;
		}
		int inicio = 0;
		for (int indiceArea = 0; indiceArea < areasSelecionadas.size(); indiceArea++) {
			int fim = inicio + areasSelecionadas.get(indiceArea).alvos().size();
			if (indiceImpacto < fim) {
				return indiceArea + 1;
			}
			inicio = fim;
		}
		return -1;
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

	public int getMovimentoReservado() {
		return movimentoReservado;
	}

	public void setMovimentoReservado(int movimentoReservado) {
		this.movimentoReservado = Math.max(0, movimentoReservado);
	}

	public Boolean getCriticoManual() {
		return criticoManual;
	}

	public void setCriticoManual(Boolean criticoManual) {
		this.criticoManual = criticoManual;
	}

}
