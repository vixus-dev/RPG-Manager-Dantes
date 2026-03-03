package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import java.util.List;

public class InvocacaoMurasame extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Ressurreição Profana";
	}

	@Override
	public String getDescricao() {
		return "Permite invocar uma criatura capturada.";
	}

	@Override
	public int getCustoMana() {
		return 2;
	} // Custo

	@Override
	public int getCustoTU() {
		return 180;
	} // Custo

	@Override
	public int getCooldownTU() {
		return 1000;
	} // Cooldown

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
		System.out.println(">>> " + conjurador.getNome() + " está invocando um lacaio!");

	}
}