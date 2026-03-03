package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.habilidades.classe.RugidoPredatório;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.*;

public class InvocacaoSangrenta extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Invocação Sangrenta";
	}

	@Override
	public String getDescricao() {
		return "Manifesta uma entidade das profundezas dependendo da necessidade.";
	}

	// Custos dinâmicos baseados na escolh
	@Override
	public int getCustoMana() {
		return 0;
	}

	@Override
	public int getCustoTU() {
		return 0;
	}

	@Override
	public int getCooldownTU() {
		return 120;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.AREA_QUADRADA;
	}

	@Override
	public int getTamanhoArea() {
		return 1;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 1;
	}

	public List<String> getOpcoesSelection() {
		return Arrays.asList("Golem", "Vigilante", "Ecos", "Portador de Selo", "Tecelão", "Dominus Albus");
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {
		String escolha = input.getOpcaoEscolhida();
		if (escolha == null)
			return;

		// Bloqueio do Dominus Albus
		if (conjurador.temPropriedade("LOCK_SUMMON")) {
			System.out.println(">>> FALHA: Selo do Dominus Albus ativo. Impossível invocar.");
			return;
		}

		Personagem invocacao = null;
		int custoMana = 0;
		int custoTU = 0;

		switch (escolha) {
		case "Golem":
			invocacao = criarSummon(conjurador, "Golem Sangrento", 1.25, 0.5, "Espada Larga", estado);
			custoMana = 2;
			custoTU = 100;
			break;
		case "Vigilante":
			invocacao = criarSummon(conjurador, "Vigilante", 0.1, 0, "Nulo", estado);
			custoMana = 5;
			custoTU = 500;
			break;
		case "Ecos":
			invocacao = criarSummon(conjurador, "Eco Umbral", 0.75, 0.75, "Garras", estado);
			invocacao.getArmaEquipada().setNomeEfeitoOnHit("Sangramento");
			invocacao.getArmaEquipada().setChanceEfeitoOnHit(0.50);
			custoMana = 2;
			custoTU = 120;
			break;
		case "Portador de Selo":
			invocacao = criarSummon(conjurador, "Portador de Selo", 0.5, 0.2, "Cajado", estado);
			invocacao.adicionarPropriedade("PORTADOR_SELO:" + conjurador.getNome());
			custoMana = 3;
			custoTU = 80;
			break;
		case "Tecelão":
			invocacao = criarSummon(conjurador, "Tecelão de Almas", 0.5, 1.5, "Agulhas", estado);
			custoMana = 3;
			custoTU = 125;
			break;
		case "Dominus Albus":
			invocacao = criarDominusAlbus(conjurador, estado);
			conjurador.adicionarPropriedade("LOCK_SUMMON");
			custoMana = 8;
			custoTU = 50;
			break;
		}

		if (invocacao != null) {
			invocacao.setPosX(input.getEpicentroX());
			invocacao.setPosY(input.getEpicentroY());
			estado.getCombatentes().add(invocacao);

			// Aplica custos manualmente pois o FN retornou 0
			conjurador.setManaAtual(conjurador.getManaAtual() - custoMana);
			conjurador.setContadorTU(conjurador.getContadorTU() + custoTU);

			manager.getMainController().atualizarInterfaceTotal();
		}
	}

	private Personagem criarSummon(Personagem mestre, String nome, double multVida, double multDano, String armaNome,
			EstadoCombate estado) {
		Map<Atributo, Integer> stats = new HashMap<>(mestre.getAtributosFinais());
		double vidaFinal = mestre.getVidaMaxima() * multVida;

		Personagem summon = new Personagem(nome, new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, vidaFinal, 10);

		summon.setFaccao(mestre.getFaccao());
		int danoBaseMestre = mestre.getArmaEquipada() != null ? mestre.getArmaEquipada().getDanoBase() : 10;

		ArmaMelee arma = new ArmaMelee(armaNome, "Invocação", "Arma de espírito", Raridade.COMUM, 0,
				(int) (danoBaseMestre * multDano), 1, Atributo.FORCA, 100, 1);
		summon.setArmaEquipada(arma);

		return summon;
	}

	private Personagem criarDominusAlbus(Personagem mestre, EstadoCombate estado) {
		Personagem dominus = criarSummon(mestre, "Dominus Albus", 2.5, 1.5, "Presas Alvas", estado);
		dominus.setMestreInvocador(mestre);

		// Injeção de Habilidades Únicas
		dominus.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.GarrasMorte());
		dominus.adicionarHabilidadeExtra(new RugidoPredatório());
		dominus.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.MordidaBestial());
		dominus.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.ConviviaUmbrarum());

		return dominus;
	}
}