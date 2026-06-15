package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TheMastersCall extends FantasmaNobre {

	public static final String MODO_ECSTASY = "Ecstasy of Gold";
	public static final String MODO_CRASH = "Crash of Worlds";
	public static final String EFEITO_ECSTASY = "The Master's Call: Ecstasy of Gold";
	public static final String EFEITO_CRASH = "The Master's Call: Crash of Worlds";
	public static final String EFEITO_GOT_LUCKY = "Got Lucky";
	public static final String CHAVE_CURA_BASE = "CURA_BASE";
	public static final int DURACAO_TU = 300;

	@Override
	public String getNome() {
		return "The Master's Call";
	}

	@Override
	public String getDescricao() {
		return "Escolha Ecstasy of Gold para criar uma aura verde de cura ou Crash of Worlds para criar uma aura vermelha que concede Got Lucky.";
	}

	@Override
	public int getCustoMana() {
		return 0;
	}

	@Override
	public int getCustoTU() {
		return 100;
	}

	@Override
	public int getCooldownTU() {
		return 757;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 21;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return Arrays.asList(MODO_ECSTASY, MODO_CRASH);
	}

	@Override
	public String getMotivoBloqueio(Personagem conjurador) {
		if (conjurador == null) {
			return null;
		}
		if (conjurador.getEfeitosAtivos().containsKey(EFEITO_ECSTASY)
				|| conjurador.getEfeitosAtivos().containsKey(EFEITO_CRASH)) {
			return "The Master's Call ja esta ativo.";
		}
		return null;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {
		String modo = input.getOpcaoEscolhida();
		if (modo == null || modo.isBlank()) {
			modo = MODO_ECSTASY;
		}

		if (MODO_CRASH.equals(modo)) {
			ativarCrashOfWorlds(conjurador, estado, manager);
		} else {
			ativarEcstasyOfGold(conjurador, estado, manager);
		}
	}

	private void ativarEcstasyOfGold(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		double curaBase = solicitarValorCuraBase(conjurador);
		Map<String, Double> modificadores = new HashMap<>();
		modificadores.put(CHAVE_CURA_BASE, curaBase);

		Efeito aura = new Efeito(EFEITO_ECSTASY, TipoEfeito.BUFF, DURACAO_TU, modificadores, 0, 0);
		manager.aplicarEfeito(conjurador, aura);
		conjurador.removerEfeito(EFEITO_CRASH);
		conjurador.recalcularAtributosEstatisticas();
		manager.atualizarAuras(estado);

		String mensagem = conjurador.getNome() + " ativou The Master's Call: Ecstasy of Gold. Cura base: "
				+ formatarValor(curaBase) + ".";
		System.out.println(">>> " + mensagem);

}

	private void ativarCrashOfWorlds(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		Efeito aura = new Efeito(EFEITO_CRASH, TipoEfeito.BUFF, DURACAO_TU, Map.of(), 0, 0);
		manager.aplicarEfeito(conjurador, aura);
		conjurador.removerEfeito(EFEITO_ECSTASY);
		conjurador.recalcularAtributosEstatisticas();
		manager.atualizarAuras(estado);

		String mensagem = conjurador.getNome() + " ativou The Master's Call: Crash of Worlds.";
		System.out.println(">>> " + mensagem);

}

	private double solicitarValorCuraBase(Personagem conjurador) {
		javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("0");
		dialog.setTitle("The Master's Call");
		dialog.setHeaderText("Ecstasy of Gold");
		dialog.setContentText("Peça aos jogadores para rodarem ENDURANCE e informe o valor fixo da cura:");

		Optional<String> resultado = dialog.showAndWait();
		if (resultado.isEmpty()) {
			return 0;
		}

		try {
			return Math.max(0, Double.parseDouble(resultado.get().trim().replace(",", ".")));
		} catch (NumberFormatException exception) {
			System.out.println(">>> Valor invalido para Ecstasy of Gold em " + conjurador.getNome() + ". Cura base 0.");
			return 0;
		}
	}

	public static int calcularRaio(Personagem conjurador) {
		if (conjurador == null) {
			return 0;
		}
		return Math.max(0, conjurador.getAtributosFinais().getOrDefault(Atributo.PERCEPCAO, 0));
	}

	public static double calcularMultiplicadorCuraPorDistancia(int distancia) {
		if (distancia <= 2) {
			return 0.75;
		}
		if (distancia == 3) {
			return 0.60;
		}
		if (distancia <= 5) {
			return 0.50;
		}
		if (distancia <= 7) {
			return 0.35;
		}
		if (distancia <= 9) {
			return 0.20;
		}
		if (distancia == 10) {
			return 0.10;
		}
		return 0;
	}

	private String formatarValor(double valor) {
		return String.format(java.util.Locale.US, "%.2f", valor);
	}
}
