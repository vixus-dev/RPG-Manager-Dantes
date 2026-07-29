package br.com.dantesrpg.model.combat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.classes.ClassePlaceholder;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.fantasmasnobres.OAnjoNascidoNoInferno;
import br.com.dantesrpg.model.habilidades.fantasmasnobres.DancaMetamoru;
import br.com.dantesrpg.model.habilidades.fantasmasnobres.Kamehameha;

/** Mantém o ciclo de vida de uma fusão gerada pela Dança Metamoru. */
public final class FusaoMetamoru {

	public static final String EFEITO_FUSAO = "Fusão Metamoru";
	private static final int DURACAO_TU = 420;

	private FusaoMetamoru() {
	}

	public record Dados(Personagem primeiro, Personagem segundo, int expiraNoTick) {
	}

	public static void criar(Personagem primeiro, Personagem segundo, EstadoCombate estado, CombatManager manager) {
		if (primeiro == null || segundo == null || estado == null || manager == null) return;
		Map<Atributo, Integer> atributosSomados = new LinkedHashMap<>();
		for (Atributo atributo : Atributo.values()) {
			atributosSomados.put(atributo, primeiro.getAtributosFinais().getOrDefault(atributo, 1)
					+ segundo.getAtributosFinais().getOrDefault(atributo, 1));
		}
		double vidaMaxima = primeiro.getVidaMaxima() + segundo.getVidaMaxima();
		Personagem fusao = new Personagem("Fusão (" + primeiro.getNome() + " + " + segundo.getNome() + ")",
				primeiro.getRaca(), new ClassePlaceholder(), Math.max(primeiro.getNivel(), segundo.getNivel()),
				new LinkedHashMap<>(atributosSomados), vidaMaxima, primeiro.getIniciativaBase());

		ajustarAtributosParaSomaExata(fusao, atributosSomados);
		fusao.setManaMaxima(Math.max(0, primeiro.getManaMaxima() + segundo.getManaMaxima()
				- fusao.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 1) / 2.0));
		fusao.setVidaAtual(Math.min(fusao.getVidaMaxima(), primeiro.getVidaAtual() + segundo.getVidaAtual()));
		fusao.setManaAtual(Math.min(fusao.getManaMaxima(), primeiro.getManaAtual() + segundo.getManaAtual()));
		fusao.setFaccao(primeiro.getFaccao());
		fusao.setPosX(primeiro.getPosX());
		fusao.setPosY(primeiro.getPosY());
		fusao.setContadorTU(20);
		fusao.setWieldingMaximo(1);
		fusao.setArmaEquipada(criarArmaFundida(primeiro.getArmaEquipada(), segundo.getArmaEquipada()));
		transferirHabilidades(primeiro, segundo, fusao);
		fusao.adicionarEfeito(new Efeito(EFEITO_FUSAO, TipoEfeito.BUFF, 999_999, null, 0, 0));

		primeiro.setAusente(true);
		segundo.setAusente(true);
		estado.getCombatentes().add(fusao);
		estado.registrarFusaoMetamoru(fusao, new Dados(primeiro, segundo, estado.getTickCounter() + DURACAO_TU));
		atualizarInterface(manager);
		System.out.println(">>> " + fusao.getNome() + " surgiu com 20 TU e permanecerá por " + DURACAO_TU + " TU.");
	}

	public static void processarExpiracoes(EstadoCombate estado, CombatManager manager) {
		if (estado == null) return;
		for (Map.Entry<Personagem, Dados> entry : List.copyOf(estado.getFusoesMetamoru().entrySet())) {
			Personagem fusao = entry.getKey();
			if (!fusao.isVivo()) {
				desfazer(fusao, estado, manager, true);
			} else if (estado.getTickCounter() >= entry.getValue().expiraNoTick()) {
				desfazer(fusao, estado, manager, false);
			}
		}
	}

	public static void desfazerAoMorrer(Personagem fusao, EstadoCombate estado, CombatManager manager) {
		desfazer(fusao, estado, manager, true);
	}

	public static boolean participaDeFusao(EstadoCombate estado, Personagem personagem) {
		if (estado == null || personagem == null) return false;
		return estado.getFusoesMetamoru().entrySet().stream().anyMatch(entry -> entry.getKey() == personagem
				|| entry.getValue().primeiro() == personagem || entry.getValue().segundo() == personagem);
	}

	public static boolean eFusao(EstadoCombate estado, Personagem personagem) {
		return estado != null && personagem != null && estado.getFusoesMetamoru().containsKey(personagem);
	}

	private static void desfazer(Personagem fusao, EstadoCombate estado, CombatManager manager, boolean morreu) {
		Dados dados = estado.removerFusaoMetamoru(fusao);
		if (dados == null) return;
		int posX = fusao.getPosX();
		int posY = fusao.getPosY();
		double percentualVida = morreu ? 0.01 : percentualAtual(fusao.getVidaAtual(), fusao.getVidaMaxima());
		double percentualMana = morreu ? 0.0 : percentualAtual(fusao.getManaAtual(), fusao.getManaMaxima());
		restaurarParticipante(dados.primeiro(), posX, posY, percentualVida, percentualMana, estado, manager);
		restaurarParticipante(dados.segundo(), posX, posY, percentualVida, percentualMana, estado, manager);
		estado.getCombatentes().remove(fusao);
		atualizarInterface(manager);
		System.out.println(">>> " + fusao.getNome() + " se desfez"
				+ (morreu ? "; os participantes retornaram com 1% de HP." : "."));
	}

	private static void restaurarParticipante(Personagem participante, int posX, int posY, double percentualVida,
			double percentualMana, EstadoCombate estado, CombatManager manager) {
		participante.setAusente(false);
		participante.setPosX(posX);
		participante.setPosY(posY);
		boolean eraProtagonista = participante.isProtagonista();
		if (eraProtagonista) participante.setProtagonista(false);
		participante.setVidaAtual(participante.getVidaMaxima() * percentualVida, estado, manager.getMainController());
		if (eraProtagonista) participante.setProtagonista(true);
		participante.setManaAtual(participante.getManaMaxima() * percentualMana);
	}

	private static double percentualAtual(double atual, double maximo) {
		return maximo <= 0 ? 0 : Math.clamp(atual / maximo, 0, 1);
	}

	private static Arma criarArmaFundida(Arma primeira, Arma segunda) {
		int dano = media(primeira == null ? 0 : primeira.getDanoBase(), segunda == null ? 0 : segunda.getDanoBase());
		int tu = media(primeira == null ? 0 : primeira.getCustoTU(), segunda == null ? 0 : segunda.getCustoTU());
		return new ArmaMelee("Arma Fundida", "Fusão", "Arma temporária da Dança Metamoru.", Raridade.UNICO,
				0, Math.max(1, dano), 1, Atributo.FORCA, Math.max(1, tu), 2);
	}

	private static int media(int primeiro, int segundo) {
		return (int) Math.round((primeiro + segundo));
	}

	private static void ajustarAtributosParaSomaExata(Personagem fusao, Map<Atributo, Integer> desejados) {
		for (Atributo atributo : Atributo.values()) {
			int atual = fusao.getAtributosFinais().getOrDefault(atributo, 1);
			int desejado = desejados.getOrDefault(atributo, 1);
			fusao.getAtributosBase().put(atributo,
					Math.max(1, fusao.getAtributosBase().getOrDefault(atributo, 1) + desejado - atual));
		}
		fusao.recalcularAtributosEstatisticas();
	}

	private static void transferirHabilidades(Personagem primeiro, Personagem segundo, Personagem fusao) {
		Set<String> existentes = fusao.getHabilidadesDeClasse().stream().map(Habilidade::getNome)
				.collect(Collectors.toSet());
		for (Habilidade habilidade : List.of(primeiro.getHabilidadesDeClasse(), segundo.getHabilidadesDeClasse()).stream()
				.flatMap(List::stream).toList()) {
			if (habilidade instanceof Kamehameha || habilidade instanceof DancaMetamoru
					|| habilidade.getNome().equals("Kamehameha") || habilidade.getNome().equals("Dança Metamoru")) {
				continue;
			}
			if (existentes.add(habilidade.getNome())) fusao.adicionarHabilidadeExtra(habilidade);
		}
		if (primeiro.getFantasmaNobre() instanceof OAnjoNascidoNoInferno
				|| segundo.getFantasmaNobre() instanceof OAnjoNascidoNoInferno) {
			// As duas técnicas já foram filtradas acima; a fusão não recebe um FN próprio.
			fusao.setFantasmaNobre(null);
		}
	}

	private static void atualizarInterface(CombatManager manager) {
		if (manager.getMainController() != null) manager.getMainController().atualizarInterfaceTotal();
	}
}
