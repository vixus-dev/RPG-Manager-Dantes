package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import java.util.List;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.Map;

public class RingOfTheUndyingWill extends FantasmaNobre {

	@Override
	public String getNome() {
		return "O Ringue da Vontade Inquebrantável";
	}

	@Override
	public String getDescricao() {
		return "Prepara um ringue de 7x7. No seu próximo turno, o ringue se ativa por 200 TU.";
	}

	@Override
	public int getCustoMana() {
		return 1;
	}

	@Override
	public int getCustoTU() {
		return 120;
	}

	@Override
	public int getCooldownTU() {
		return 600;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 8;
	} // Guarda o tamanho (7x7) para o CombatManager ler

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " começa a canalizar O RINGUE DA VONTADE INQUEBRÁVEL!");

		Efeito efeitoPreparando = new Efeito("Ringue (Preparando)", // Efeito "Flag"
				TipoEfeito.BUFF, 9999, // Duração "infinita" (será removido manualmente)
				Map.of(), 0, 0);
		conjurador.adicionarEfeito(efeitoPreparando);
		conjurador.recalcularAtributosEstatisticas();
	}
}