package br.com.dantesrpg.model.habilidades.fantasmasnobres;

import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;

/** Feixe de energia carregável concedido por O Anjo Nascido no Inferno. */
public class Kamehameha extends Habilidade {

	public static final String EFEITO_CARREGANDO = "Kamehameha: Carregando";
	public static final String PROPRIEDADE_CARGA = "KAMEHAMEHA_CARGA";
	private static final String PROPRIEDADE_RESTANTES = "KAMEHAMEHA_RESTANTES";
	private static final int TU_DISPARO = 120;
	private static final int TU_POR_CARGA = 70;
	private int cargaSelecionada;
	private int cargaDoDisparo;
	private boolean emMira;

	public Kamehameha() {
		super("Kamehameha",
				"Dispara um feixe de Inspiração. Escolha de 0 a 4 cargas; cada carga soma +0,75x de dano. "
						+ "A cada 2 cargas, a linha ganha +2 de alcance e +1 de largura.",
				br.com.dantesrpg.model.enums.TipoHabilidade.ATIVA, 2, TU_DISPARO, 1,
				TipoAlvo.LINHA, 1, 0.125, 10, List.of());
	}

	public void selecionarCarga(int carga) {
		if (emMira) return;
		cargaSelecionada = Math.clamp(carga, 0, 4);
	}

	public void prepararMira(int carga) {
		cargaDoDisparo = Math.clamp(carga, 0, 4);
		emMira = true;
	}

	public boolean estaEmMira() {
		return emMira;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return emMira || cargaSelecionada == 0 ? TipoAlvo.LINHA : TipoAlvo.SI_MESMO;
	}

	@Override
	public int getCustoTUModificado(Personagem conjurador) {
		return emMira || cargaSelecionada == 0 ? TU_DISPARO : TU_POR_CARGA;
	}

	@Override
	public int getTamanhoArea() {
		return 1 + (getCargaEfetiva() / 2);
	}

	@Override
	public int getAlcanceMaximo() {
		return 4 + (2 * (getCargaEfetiva() / 2));
	}

	@Override
	public double getMultiplicadorDeDano() {
		if (!emMira && cargaSelecionada > 0) return 0;
		return 0.125 + (0.075 * getCargaEfetiva());
	}

	@Override
	public double getMultiplicadorModificado(Personagem ator, Personagem alvo, EstadoCombate estado) {
		return getMultiplicadorDeDano();
	}

	@Override
	public List<String> getOpcoesSelection() {
		return emMira ? List.of() : List.of("0", "1", "2", "3", "4");
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (emMira) {
			System.out.println(">>> " + conjurador.getNome() + " descarrega Kamehameha com " + cargaDoDisparo
					+ " carga(s): " + String.format("%.2fx", getMultiplicadorDeDano()) + ".");
			emMira = false;
			cargaSelecionada = 0;
			cargaDoDisparo = 0;
			return;
		}
		if (cargaSelecionada <= 0) {
			System.out.println(">>> " + conjurador.getNome() + " dispara Kamehameha sem carga.");
			return;
		}

		Efeito carregando = new Efeito(EFEITO_CARREGANDO, TipoEfeito.BUFF, 9_999,
				null, 0, 0);
		carregando.setStacks(cargaSelecionada);
		manager.getEffectProcessor().aplicarEfeito(conjurador, carregando);
		conjurador.getPropriedades().removeIf(propriedade -> propriedade.startsWith(PROPRIEDADE_CARGA + ":"));
		conjurador.getPropriedades().add(PROPRIEDADE_CARGA + ":" + cargaSelecionada);
		conjurador.getPropriedades().removeIf(propriedade -> propriedade.startsWith(PROPRIEDADE_RESTANTES + ":"));
		conjurador.getPropriedades().add(PROPRIEDADE_RESTANTES + ":" + cargaSelecionada);
		System.out.println(">>> " + conjurador.getNome() + " começa a carregar Kamehameha por "
				+ cargaSelecionada + " turno(s), recebendo " + TU_POR_CARGA + " TU por carga.");
	}

	/**
	 * Processa uma passagem automática de turno durante a carga.
	 *
	 * @return true quando o turno deve ser pulado.
	 */
	public static boolean processarTurnoCarregando(Personagem personagem) {
		Efeito efeito = personagem.getEfeitosAtivos().get(EFEITO_CARREGANDO);
		int restantes = personagem.getValorPropriedade(PROPRIEDADE_RESTANTES);
		if (restantes <= 0 && efeito == null) return false;
		if (restantes <= 0) restantes = Math.max(1, efeito.getStacks());
		if (restantes == 1) {
			personagem.removerEfeito(EFEITO_CARREGANDO);
			int carga = personagem.getValorPropriedade(PROPRIEDADE_CARGA);
			personagem.getPropriedades().removeIf(propriedade -> propriedade.startsWith(PROPRIEDADE_CARGA + ":"));
			personagem.getPropriedades().removeIf(propriedade -> propriedade.startsWith(PROPRIEDADE_RESTANTES + ":"));
			prepararMira(personagem, Math.max(1, carga));
			return false;
		}

		personagem.getPropriedades().removeIf(propriedade -> propriedade.startsWith(PROPRIEDADE_RESTANTES + ":"));
		personagem.getPropriedades().add(PROPRIEDADE_RESTANTES + ":" + (restantes - 1));
		if (efeito != null) efeito.setStacks(restantes - 1);
		personagem.setContadorTU(personagem.getContadorTU() + TU_POR_CARGA);
		System.out.println(">>> " + personagem.getNome() + " continua carregando Kamehameha ("
				+ (restantes - 1) + " turno(s) restante(s)) e passa a vez (+" + TU_POR_CARGA + " TU).");
		return true;
	}

	/** Recebe a carga preservada quando o marcador de carga termina. */
	public static void prepararMira(Personagem personagem, int carga) {
		if (personagem.getFantasmaNobre() instanceof br.com.dantesrpg.model.fantasmasnobres.OAnjoNascidoNoInferno anjo) {
			anjo.prepararMiraKamehameha(carga);
		}
	}

	private int getCargaEfetiva() {
		return emMira ? cargaDoDisparo : cargaSelecionada;
	}
}
