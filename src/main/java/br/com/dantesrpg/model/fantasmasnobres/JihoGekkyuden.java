package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;

import java.util.List;
import java.util.Map;

/**
 * Fantasma Nobre de Lillith — "Jihō Gekkyūden".
 *
 * Custo: 0 mana, 30 TU. O domínio 7x7 entra no turno seguinte (padrão delayed via
 * efeito "(Preparando)"), dura 500 TU. Ao expirar, Lillith recebe "Exausto pelo
 * Jihō Gekkyūden" por 100 TU. Cooldown de 600 TU no uso do FN.
 *
 * Dentro do domínio:
 *   - Aliados (mesma facção da dona): "Benção do Gekkyūden" — +50% dano e +1 escudo de sangue a cada 5 TU.
 *   - Inimigos: "Maldição do Gekkyūden" — −2 movimento e 1 dano a cada 20 TU.
 *
 * A aplicação/remoção dos efeitos por tile é feita pelo AuraManager.
 * Os ticks especiais (escudo/dano) são feitos pelo CombatManager.
 */
public class JihoGekkyuden extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Jihō Gekkyūden";
	}

	@Override
	public String getDescricao() {
		return "Expansão de Domínio (7x7). Converte a área em sangue negro por 500 TU. "
				+ "Aliados ganham +50% dano e +1 escudo de sangue a cada 5 TU; "
				+ "inimigos sofrem -2 movimento e 1 de dano a cada 20 TU. "
				+ "Após o domínio se dissipar, Lillith fica exausta por 100 TU.";
	}

	@Override
	public int getCustoMana() {
		return 0;
	}

	@Override
	public int getCustoTU() {
		return 30;
	}

	@Override
	public int getCooldownTU() {
		return 600; // 500 TU de domínio + 100 TU de exaustão
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 7;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " prepara a EXPANSÃO DE DOMÍNIO: JIHŌ GEKKYŪDEN!");

		Efeito preparando = new Efeito("Jihō Gekkyūden (Preparando)", TipoEfeito.BUFF, 9999, Map.of(), 0, 0);
		conjurador.adicionarEfeito(preparando);
		conjurador.recalcularAtributosEstatisticas();
	}
}
