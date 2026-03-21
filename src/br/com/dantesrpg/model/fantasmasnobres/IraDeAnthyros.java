package br.com.dantesrpg.model.fantasmasnobres;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;

public class IraDeAnthyros extends FantasmaNobre {

	private static final String EFEITO_IRA = "Ira de Anthyros";
	private static final String EFEITO_FURIA = "Furia de Anthyros";

	private static final int MAX_ACUMULOS = 5;
	private static final double BONUS_DANO_POR_ACUMULO = 0.20;

	// O motor consome duracao em dois pontos do turno, entao os valores sao
	// armazenados em dobro para refletir os TUs exibidos ao jogador.
	private static final int DURACAO_PASSIVO = 999999;
	private static final int DURACAO_FURIA = 1200;

	@Override
	public String getNome() {
		return "Furia de Anthyros";
	}

	@Override
	public String getDescricao() {
		return "Passivo: cada golpe gera 1 acumulo de Ira. Com 5 acumulacoes, pode ativar a Furia de Anthyros por 600 TU, recebendo +2 EN, +2 SA, +50 Armadura e +20% de dano por acumulo atual.";
	}

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
		return 0;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 0;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {
		garantirEfeitoIra(conjurador);

		String motivoBloqueio = getMotivoBloqueio(conjurador);
		if (motivoBloqueio != null) {
			System.out.println(">>> " + motivoBloqueio);
			return;
		}

		aplicarFuria(conjurador);
	}

	@Override
	public void onCombatStart(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		garantirEfeitoIra(conjurador);
		atualizarBonusFuria(conjurador);
	}

	@Override
	public void onTurnStart(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		garantirEfeitoIra(conjurador);

		if (!furiaAtiva(conjurador)) {
			return;
		}

		int acumulosAtuais = getAcumulos(conjurador);
		int novosAcumulos = Math.max(0, acumulosAtuais - 1);
		if (novosAcumulos != acumulosAtuais) {
			definirAcumulos(conjurador, novosAcumulos);
			System.out.println(">>> " + conjurador.getNome() + " perde 1 acumulo de Ira no inicio do turno ("
					+ novosAcumulos + "/" + MAX_ACUMULOS + ").");
		} else {
			atualizarBonusFuria(conjurador);
		}
	}

	@Override
	public void onDamageDealt(Personagem conjurador, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatManager manager) {
		if (furiaAtiva(conjurador)) {
			return;
		}

		adicionarAcumulos(conjurador, 1);
	}

	@Override
	public void onCriticalHit(Personagem conjurador, Personagem alvo, EstadoCombate estado, CombatManager manager) {
		if (!furiaAtiva(conjurador)) {
			return;
		}

		adicionarAcumulos(conjurador, 1);
	}

	@Override
	public String getMotivoBloqueio(Personagem conjurador) {
		if (conjurador == null) {
			return "Fantasma nobre indisponivel.";
		}

		if (furiaAtiva(conjurador)) {
			return conjurador.getNome() + " ja esta transformado em Furia de Anthyros.";
		}

		if (getAcumulos(conjurador) < MAX_ACUMULOS) {
			return conjurador.getNome() + " precisa de 5 acumulacoes de Ira para ativar a Furia de Anthyros.";
		}

		return null;
	}

	private void garantirEfeitoIra(Personagem personagem) {
		Efeito ira = personagem.getEfeitosAtivos().get(EFEITO_IRA);
		if (ira != null) {
			ira.setDuracaoTURestante(DURACAO_PASSIVO);
			return;
		}

		Efeito efeitoIra = new Efeito(EFEITO_IRA, TipoEfeito.BUFF, DURACAO_PASSIVO, new HashMap<>(), 0, 0);
		efeitoIra.setStacks(0);
		personagem.adicionarEfeito(efeitoIra);
	}

	private boolean furiaAtiva(Personagem personagem) {
		return personagem.getEfeitosAtivos().containsKey(EFEITO_FURIA);
	}

	private int getAcumulos(Personagem personagem) {
		Efeito ira = personagem.getEfeitosAtivos().get(EFEITO_IRA);
		if (ira == null) {
			return 0;
		}

		return Math.max(0, Math.min(MAX_ACUMULOS, ira.getStacks()));
	}

	private void adicionarAcumulos(Personagem personagem, int quantidade) {
		if (quantidade <= 0) {
			return;
		}

		garantirEfeitoIra(personagem);

		int acumulosAtuais = getAcumulos(personagem);
		int novosAcumulos = Math.min(MAX_ACUMULOS, acumulosAtuais + quantidade);
		if (novosAcumulos == acumulosAtuais) {
			return;
		}

		definirAcumulos(personagem, novosAcumulos);

		if (furiaAtiva(personagem)) {
			System.out.println(">>> Critico! " + personagem.getNome() + " recupera 1 acumulo de Ira (" + novosAcumulos
					+ "/" + MAX_ACUMULOS + ").");
		} else if (novosAcumulos >= MAX_ACUMULOS) {
			System.out.println(">>> " + personagem.getNome()
					+ " chegou ao maximo de Ira e pode ativar a Furia de Anthyros!");
		} else {
			System.out.println(">>> " + personagem.getNome() + " ganhou Ira (" + novosAcumulos + "/" + MAX_ACUMULOS
					+ ").");
		}
	}

	private void definirAcumulos(Personagem personagem, int acumulos) {
		garantirEfeitoIra(personagem);

		Efeito ira = personagem.getEfeitosAtivos().get(EFEITO_IRA);
		if (ira == null) {
			return;
		}

		ira.setStacks(Math.max(0, Math.min(MAX_ACUMULOS, acumulos)));
		ira.setDuracaoTURestante(DURACAO_PASSIVO);
		atualizarBonusFuria(personagem);
	}

	private void aplicarFuria(Personagem personagem) {
		Efeito furia = new Efeito(EFEITO_FURIA, TipoEfeito.BUFF, DURACAO_FURIA, criarModsFuria(getAcumulos(personagem)),
				0, 0);
		personagem.adicionarEfeito(furia);
		atualizarBonusFuria(personagem);
		System.out.println(">>> " + personagem.getNome() + " entrou em Furia de Anthyros!");
	}

	private void atualizarBonusFuria(Personagem personagem) {
		Efeito furia = personagem.getEfeitosAtivos().get(EFEITO_FURIA);
		if (furia == null) {
			personagem.recalcularAtributosEstatisticas();
			return;
		}

		Map<String, Double> modificadores = furia.getModificadores();
		if (modificadores == null) {
			return;
		}

		modificadores.clear();
		modificadores.putAll(criarModsFuria(getAcumulos(personagem)));
		personagem.recalcularAtributosEstatisticas();
	}

	private Map<String, Double> criarModsFuria(int acumulos) {
		Map<String, Double> mods = new HashMap<>();
		mods.put("ENDURANCE", 2.0);
		mods.put("SAGACIDADE", 2.0);
		mods.put("ARMADURA_TOTAL", 50.0);
		mods.put("DANO_BONUS_PERCENTUAL", acumulos * BONUS_DANO_POR_ACUMULO);
		return mods;
	}
}
