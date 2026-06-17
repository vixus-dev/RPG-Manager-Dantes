package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.List;

public class RevelacaoDeYaweh extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Revelação de Yaweh";
	}

	@Override
	public String getDescricao() {
		return "O Fantasma Nobre de Sarvant, atrelado ao seu grimório sagrado YAWEH. (Base)";
	}

	@Override
	public int getCustoMana() {
		return 4; // Custo Mana padrão
	}

	@Override
	public int getCustoTU() {
		return 150; // Custo TU padrão
	}

	@Override
	public int getCooldownTU() {
		return 500; // Cooldown padrão
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
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " ativa o Fantasma Nobre: Revelação de Yaweh!");
		// Implementação base para posterior expansão.
	}
}
