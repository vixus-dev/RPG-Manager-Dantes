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

public class LuaSombria extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Lua Sombria";
	}

	@Override
	public String getDescricao() {
		return "Expansão de Domínio (7x7). Abre um domínio de escuridão por 1000 TU. "
				+ "Durante esse tempo, todos os seus custos de TU são cortados pela metade.";
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
		return 1000;
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
		System.out.println(">>> " + conjurador.getNome() + " prepara a EXPANSÃO DE DOMÍNIO: LUA SOMBRIA!");

		Efeito preparando = new Efeito("Lua Sombria (Preparando)", TipoEfeito.BUFF, 9999, Map.of(), 0, 0);
		conjurador.adicionarEfeito(preparando);
		conjurador.recalcularAtributosEstatisticas();
	}
}
