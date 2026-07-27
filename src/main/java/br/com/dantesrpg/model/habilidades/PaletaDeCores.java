package br.com.dantesrpg.model.habilidades;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

/** Habilidade concedida pelo Pincel Gigante. */
public class PaletaDeCores extends Habilidade {
	public static final String NOME = "Paleta de Cores";
	public static final String DADO_INSPIRACAO = "DADO_ATRIBUTO";
	public static final String VERMELHO = "Vermelho";
	public static final String AZUL = "Azul";
	public static final String AMARELO = "Amarelo";
	public static final String PRETO = "Preto";

	private String opcaoSelecionada = VERMELHO;
	private transient AcaoMestreInput ultimaEntradaExecutada;

	public PaletaDeCores() {
		super(NOME,
				"Escolha uma cor e role Inspiração: Vermelho corta cura e armadura; Azul cura; "
						+ "Amarelo imobiliza uma área; Preto causa dano em linha e aplica Descolorido.",
				TipoHabilidade.ATIVA, 2, 100, 1, TipoAlvo.INDIVIDUAL, 0, 0.0, 0,
				Collections.emptyList());
	}

	public void setOpcaoSelecionada(String opcao) {
		if (getOpcoesSelection().contains(opcao)) {
			this.opcaoSelecionada = opcao;
		}
	}

	public String getOpcaoSelecionada() {
		return opcaoSelecionada;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return List.of(VERMELHO, AZUL, AMARELO, PRETO);
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		if (AMARELO.equals(opcaoSelecionada)) return TipoAlvo.AREA_QUADRADA;
		if (PRETO.equals(opcaoSelecionada)) return TipoAlvo.LINHA;
		return TipoAlvo.INDIVIDUAL;
	}

	@Override
	public int getTamanhoArea() {
		return AMARELO.equals(opcaoSelecionada) ? 3 : 0;
	}

	@Override
	public double getMultiplicadorDeDano() {
		return PRETO.equals(opcaoSelecionada) ? 0.75 : 0.0;
	}

	@Override
	public int getTicksDeDano() {
		return PRETO.equals(opcaoSelecionada) ? 1 : 0;
	}

	@Override
	public int getAlcanceMaximo() {
		return PRETO.equals(opcaoSelecionada) ? 4 : 3;
	}

	@Override
	public int getAnguloCone() {
		return 0;
	}

	@Override
	public boolean afetaInimigos() {
		return !AZUL.equals(opcaoSelecionada);
	}

	@Override
	public boolean afetaAliados() {
		return AZUL.equals(opcaoSelecionada);
	}

	@Override
	public boolean afetaSiMesmo() {
		return AZUL.equals(opcaoSelecionada);
	}

	@Override
	public String getMotivoBloqueio(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado) {
		if (conjurador == null) {
			return "Paleta de Cores sem conjurador.";
		}
		if (alvos == null || alvos.isEmpty()) {
			return "Selecione pelo menos um alvo para a Paleta de Cores.";
		}
		boolean aliado = alvos.get(0) != null && conjurador.getFaccao().equals(alvos.get(0).getFaccao());
		if (AZUL.equals(opcaoSelecionada) && !aliado) {
			return "A cor Azul só pode atingir aliados ou o próprio usuário.";
		}
		if (!AZUL.equals(opcaoSelecionada) && aliado) {
			return "Esta cor só pode atingir inimigos.";
		}
		return null;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		AcaoMestreInput input = manager != null ? manager.getLastInput() : null;
		if (input != null && input == ultimaEntradaExecutada) {
			return;
		}
		if (input != null) {
			ultimaEntradaExecutada = input;
		}
		String cor = input != null && getOpcoesSelection().contains(input.getOpcaoEscolhida())
				? input.getOpcaoEscolhida() : opcaoSelecionada;
		int dado = input != null ? input.getResultadoDado(DADO_INSPIRACAO) : -1;
		if (dado < 0 || alvos == null || alvos.isEmpty() || manager == null) {
			return;
		}

		switch (cor) {
		case VERMELHO -> executarVermelho(alvos.get(0), dado, manager);
		case AZUL -> executarAzul(alvos.get(0), dado, estado, manager);
		case AMARELO -> executarAmarelo(alvos, dado, manager);
		case PRETO -> executarPreto(conjurador, alvos, dado, manager);
		default -> { }
		}
	}

	private void executarVermelho(Personagem alvo, int dado, CombatManager manager) {
		Efeito vermelho = new Efeito("Paleta Vermelha", TipoEfeito.DEBUFF, 200,
				Map.of("REDUCAO_CURA", 0.30, "ARMADURA_TOTAL", -2.0 * dado), 0, 0);
		manager.getEffectProcessor().aplicarEfeito(alvo, vermelho);
	}

	private void executarAzul(Personagem alvo, int dado, EstadoCombate estado, CombatManager manager) {
		double cura = alvo.getVidaMaxima() * (dado * 0.05);
		double espacoAntes = Math.max(0.0, alvo.getVidaMaxima() - alvo.getVidaAtual());
		alvo.curarIgnorandoBloqueios(cura, estado, manager.getMainController());
		double curaAplicada = Math.max(0.0, alvo.getVidaAtual() - (alvo.getVidaMaxima() - espacoAntes));
		double sobrecura = Math.max(0.0, cura - Math.min(cura, espacoAntes));
		if (sobrecura > 0) {
			alvo.adicionarEscudoSangue(sobrecura * 1.25);
		}
		System.out.println(">>> Paleta Azul: " + alvo.getNome() + " recebeu "
				+ String.format("%.1f", curaAplicada) + " de cura e "
				+ String.format("%.1f", sobrecura * 1.25) + " de escudo de sangue.");
	}

	private void executarAmarelo(List<Personagem> alvos, int dado, CombatManager manager) {
		for (Personagem alvo : alvos) {
			if (alvo == null) continue;
			manager.getEffectProcessor().aplicarEfeito(alvo,
					new Efeito("Imobilizado", TipoEfeito.DEBUFF, 80, Map.of(), 0, 0));
			manager.getEffectProcessor().aplicarEfeito(alvo,
					new Efeito("Paleta Amarela", TipoEfeito.DOT, 100, Map.of(),
							Math.max(1, (int) Math.ceil(dado * 2.25)), 100));
		}
	}

	private void executarPreto(Personagem conjurador, List<Personagem> alvos, int dado, CombatManager manager) {
		for (Personagem alvo : alvos) {
			if (alvo == null) continue;
			int danoBase = manager.estimarDano(conjurador, this, alvo, dado,
					manager.getLastInput().getResultadoDado("DADO_ATRIBUTO_NATURAL"), 0);
			int danoTick = Math.max(1, (int) Math.ceil(danoBase * 0.40));
			manager.getEffectProcessor().aplicarEfeito(alvo,
					new Efeito("Descolorido", TipoEfeito.DOT, 200, Map.of(), danoTick, 50));
		}
	}
}
