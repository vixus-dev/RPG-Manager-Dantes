package br.com.dantesrpg.controller.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.fantasmasnobres.AcertoDeContas;
import br.com.dantesrpg.model.fantasmasnobres.AndJusticeForMySelf;
import br.com.dantesrpg.model.fantasmasnobres.ApostadorIncansavel;
import br.com.dantesrpg.model.fantasmasnobres.GodsWill;
import br.com.dantesrpg.model.fantasmasnobres.Fimbulwinter;
import br.com.dantesrpg.model.fantasmasnobres.InvocacaoMurasame;
import br.com.dantesrpg.model.fantasmasnobres.InvocacaoSangrenta;
import br.com.dantesrpg.model.fantasmasnobres.IraDeAnthyros;
import br.com.dantesrpg.model.fantasmasnobres.JihoGekkyuden;
import br.com.dantesrpg.model.fantasmasnobres.LuaSombria;
import br.com.dantesrpg.model.fantasmasnobres.ModoPolaris;
import br.com.dantesrpg.model.fantasmasnobres.ProfetaDeBehemoth;
import br.com.dantesrpg.model.fantasmasnobres.RingOfTheUndyingWill;
import br.com.dantesrpg.model.fantasmasnobres.Ritual;
import br.com.dantesrpg.model.fantasmasnobres.TheMastersCall;
import br.com.dantesrpg.model.fantasmasnobres.VigiliaEterna;
import br.com.dantesrpg.model.fantasmasnobres.RevelacaoDeYaweh;
import br.com.dantesrpg.model.items.EssenciaInimigo;
public class FantasmaNobreActionService {

	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<CombatManager> combatManagerSupplier;
	private final Supplier<MapController> primaryMapSupplier;
	private final Consumer<Consumer<MapController>> forEachMap;
	private final Runnable atualizarInterfaceRoster;
	private final Runnable fecharHudEAvancar;

	public FantasmaNobreActionService(Supplier<EstadoCombate> estadoSupplier,
			Supplier<CombatManager> combatManagerSupplier, Supplier<MapController> primaryMapSupplier,
			Consumer<Consumer<MapController>> forEachMap, Runnable atualizarInterfaceRoster,
			Runnable fecharHudEAvancar) {
		this.estadoSupplier = estadoSupplier;
		this.combatManagerSupplier = combatManagerSupplier;
		this.primaryMapSupplier = primaryMapSupplier;
		this.forEachMap = forEachMap;
		this.atualizarInterfaceRoster = atualizarInterfaceRoster;
		this.fecharHudEAvancar = fecharHudEAvancar;
	}

	public void resolverAcaoFantasmaNobre(AcaoMestreInput input) {
		EstadoCombate estado = estadoSupplier.get();
		CombatManager combatManager = combatManagerSupplier.get();
		if (input.getAtor() != estado.getAtorAtual() || !estado.isCombateAtivo()) {
			return;
		}

		Personagem ator = input.getAtor();
		FantasmaNobre fantasmaNobre = ator.getFantasmaNobre();
		if (fantasmaNobre == null) {
			System.err.println("Erro: " + ator.getNome() + " tentou usar FN, mas nao tem um FN equipado.");
			return;
		}

		String cooldownEffectNameFn = "CD:" + fantasmaNobre.getNome();
		if (ator.getEfeitosAtivos().containsKey(cooldownEffectNameFn)) {
			System.out.println(">>> " + fantasmaNobre.getNome() + " ainda esta em recarga.");
			return;
		}

		if (ator.getManaAtual() < fantasmaNobre.getCustoMana()) {
			System.out.println(">>> " + ator.getNome() + " nao tem mana suficiente para usar "
					+ fantasmaNobre.getNome() + ".");
			return;
		}

		String motivoBloqueio = fantasmaNobre.getMotivoBloqueio(ator);
		if (motivoBloqueio != null) {
			System.out.println(">>> " + motivoBloqueio);
			return;
		}

		System.out.println(">>> " + ator.getNome() + " está ativando: " + fantasmaNobre.getNome() + "!");
		fantasmaNobre.executar(ator, input.getAlvos(), estado, input, combatManager);
		aplicarCustosCooldownEHook(ator, fantasmaNobre, combatManager);
		fecharHudEAvancar.run();
	}

	public void resolverAcaoInvocacao(Personagem ator, FantasmaNobre fantasmaNobre, EssenciaInimigo essencia) {
		EstadoCombate estado = estadoSupplier.get();
		CombatManager combatManager = combatManagerSupplier.get();
		if (ator != estado.getAtorAtual() || !estado.isCombateAtivo()) {
			return;
		}

		System.out.println(">>> " + ator.getNome() + " está invocando " + essencia.getNome() + "!");

ator.getInventario().removerItem(essencia);

		Personagem servoInvocado = criarServo(ator, essencia);
		adicionarInvocacaoAoCombate(servoInvocado, ator);
		aplicarCustosCooldownEHook(ator, fantasmaNobre, combatManager);
		fecharHudEAvancar.run();
	}

	public FantasmaNobre instanciarFantasmaNobre(String nomeExibicao) {
		if (nomeExibicao == null) {
			return null;
		}
		switch (nomeExibicao) {
			case "AcertoDeContas":
				return new AcertoDeContas();
			case "AndJusticeForMySelf":
				return new AndJusticeForMySelf();
			case "ApostadorIncansavel":
				return new ApostadorIncansavel();
			case "GodsWill":
				return new GodsWill();
			case "Fimbulwinter":
				return new Fimbulwinter();
			case "InvocacaoMurasame":
				return new InvocacaoMurasame();
			case "InvocacaoSangrenta":
				return new InvocacaoSangrenta();
			case "IraDeAnthyros":
				return new IraDeAnthyros();
			case "JihoGekkyuden":
				return new JihoGekkyuden();
			case "LuaSombria":
				return new LuaSombria();
			case "ModoPolaris":
				return new ModoPolaris();
			case "ProfetaDeBehemoth":
				return new ProfetaDeBehemoth();
			case "RevelacaoDeYaweh":
				return new RevelacaoDeYaweh();
			case "RingOfTheUndyingWill":
				return new RingOfTheUndyingWill();
			case "Ritual":
				return new Ritual();
			case "TheMastersCall":
				return new TheMastersCall();
			case "VigiliaEterna":
				return new VigiliaEterna();
			default:
				return null;
		}
	}

	private void aplicarCustosCooldownEHook(Personagem ator, FantasmaNobre fantasmaNobre, CombatManager combatManager) {
		int custoManaFinal = fantasmaNobre.getCustoMana();
		int custoTUFinal = fantasmaNobre.getCustoTU();
		ator.setManaAtual(ator.getManaAtual() - custoManaFinal);
		ator.setContadorTU(ator.getContadorTU() + custoTUFinal);
		System.out.println(ator.getNome() + " gasta " + custoManaFinal + " MP e " + custoTUFinal + " TUs.");

		int cooldown = fantasmaNobre.getCooldownTU();
		if (cooldown > 0) {
			String cooldownEffectName = "CD:" + fantasmaNobre.getNome();
			Efeito cooldownEfeito = new Efeito(cooldownEffectName, TipoEfeito.DEBUFF, cooldown, null, 0, 0);
			combatManager.aplicarEfeito(ator, cooldownEfeito);
			System.out.println(">>> " + fantasmaNobre.getNome() + " entrou em cooldown por " + cooldown + " TU.");
		}

		combatManager.setUltimoTipoAcao(ator, TipoAcao.FANTASMA_NOBRE);
	}

	private Personagem criarServo(Personagem invocador, EssenciaInimigo essencia) {
		String nomeOriginal = essencia.getNomeInimigoOriginal();
		Map<Atributo, Integer> statsBase = essencia.getAtributosBaseInimigo();
		double vidaBase = essencia.getVidaMaximaBaseInimigo();
		Arma armaOriginal = essencia.getArmaInimigo();

		int inteligencia = invocador.getAtributosFinais().getOrDefault(Atributo.INTELIGENCIA, 0);
		double modStats = 0.05 * inteligencia;
		System.out.println(">>> Invocando Servo com " + (modStats * 100) + "% dos stats originais.");

		Map<Atributo, Integer> statsServo = new HashMap<>();
		for (Map.Entry<Atributo, Integer> entry : statsBase.entrySet()) {
			int statModificada = (int) (entry.getValue() * modStats);
			statsServo.put(entry.getKey(), Math.max(1, statModificada));
		}
		int vidaServo = Math.max(10, (int) (vidaBase * modStats));

		Personagem servo = new Personagem("Servo: " + nomeOriginal,
				new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, statsServo, vidaServo, 10);
		servo.setArmaEquipada(armaOriginal);
		servo.setFaccao("JOGADOR");
		servo.recalcularAtributosEstatisticas();
		servo.setVidaAtual(servo.getVidaMaxima());
		servo.setManaAtual(servo.getManaMaxima());
		return servo;
	}

	private void adicionarInvocacaoAoCombate(Personagem invocacao, Personagem invocador) {
		if (invocacao == null) {
			return;
		}

		MapController mapaBusca = primaryMapSupplier.get();
		if (mapaBusca != null) {
			javafx.util.Pair<Integer, Integer> posLivre = mapaBusca
					.encontrarCelulaLivreMaisProxima(invocador.getPosX(), invocador.getPosY());
			if (posLivre != null) {
				invocacao.setPosX(posLivre.getKey());
				invocacao.setPosY(posLivre.getValue());
			} else {
				System.out.println(
						">>> AVISO: Invocação sem espaço. Colocando no lugar do invocador (Sobreposição de emergência).");
				invocacao.setPosX(invocador.getPosX());
				invocacao.setPosY(invocador.getPosY());
			}
		}

		EstadoCombate estado = estadoSupplier.get();
		invocacao.setContadorTU(invocador.getContadorTU() + 1);
		estado.getCombatentes().add(invocacao);
		atualizarInterfaceRoster.run();
		forEachMap.accept(m -> m.desenharPeoes(estado.getCombatentes()));
		System.out.println(">>> " + invocacao.getNome() + " foi adicionado ao combate!");
	}
}
