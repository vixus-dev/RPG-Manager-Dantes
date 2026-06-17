package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ProtecaoDosCeus extends Habilidade {

	public ProtecaoDosCeus() {
		super("Proteção dos Céus",
			  "Com o escudo do senhor o jogador selecionado é protegido durante 3 turnos com o escudo tendo 20 de hp por turno.",
			  TipoHabilidade.ATIVA,
			  2, // Custo Mana
			  90, // Custo TU
			  1, // Nível Necessário
			  TipoAlvo.INDIVIDUAL,
			  0.0, // Multiplicador de dano
			  0, // Ticks de dano
			  Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador == null || alvos == null || alvos.isEmpty() || manager == null) {
			return;
		}

		Personagem target = alvos.get(0);
		if (target == null || !target.isAtivoNoCombate()) {
			return;
		}

		System.out.println(">>> " + conjurador.getNome() + " usa Proteção dos Céus em " + target.getNome() + ".");

		// Cria o efeito "Proteção dos Céus" por 500 TU
		Map<String, Double> mods = new HashMap<>();
		mods.put("TAXA_CRITICA", 0.0);
		Efeito buff = new Efeito("Proteção dos Céus", TipoEfeito.BUFF, 500, mods, 0, 0);

		target.adicionarEfeito(buff);

		// Configura o escudo inicial de 20 HP
		target.setEscudoDivinoMaximo(20.0);
		target.setEscudoDivinoAtual(20.0);

		// Inicializa propriedade para verificar mudanças no HP do escudo
		target.getPropriedades().removeIf(prop -> prop.startsWith("ProtecaoCeusLastHP:"));
		target.adicionarPropriedade("ProtecaoCeusLastHP:20");

		target.recalcularAtributosEstatisticas();
	}
}
