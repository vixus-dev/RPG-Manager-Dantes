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

public class AcertoDeContas extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Acerto de Contas";
	}

	@Override
	public String getDescricao() {
		return "Prepara um 'Tiro Especial' que buffa seu próximo ataque.";
	}

	@Override
	public int getCustoMana() {
		return 3;
	}

	@Override
	public int getCustoTU() {
		return 100;
	}

	@Override
	public int getCooldownTU() {
		return 300;
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
		System.out.println(">>> " + conjurador.getNome() + " ativa ACERTO DE CONTAS!");

		Efeito buffTiroEspecial = new Efeito("Tiro Especial", // Nome do Efeito
				TipoEfeito.BUFF, // Tipo (para o ícone azul)
				9999, // Duração (será removido ao usar)
				Map.of(), // Não aplica stats passivos
				0, 0 // Não é DoT
		);
		// Aplica no conjurador
		conjurador.adicionarEfeito(buffTiroEspecial);
		System.out.println(">>> " + conjurador.getNome() + " está com [Tiro Especial] pronto!");

		conjurador.recalcularAtributosEstatisticas(); // Para a UI mostrar o novo buff
	}
}