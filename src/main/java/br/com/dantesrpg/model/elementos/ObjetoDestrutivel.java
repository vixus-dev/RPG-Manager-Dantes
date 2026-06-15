package br.com.dantesrpg.model.elementos;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.classes.ClassePlaceholder;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.racas.RaçaPlaceholder;
import java.util.HashMap;
import java.util.Map;

public class ObjetoDestrutivel extends Personagem {

	private boolean bloqueiaVisao;

	public ObjetoDestrutivel(String nome, int hpMax, int defesa, boolean bloqueiaVisao) {
		// Construtor "Fake" para satisfazer a classe Personagem
		super(nome, new RaçaPlaceholder(), new ClassePlaceholder(), 1, criarAtributosFixos(defesa), hpMax, 0 // Iniciativa
																												// 0
		);

		this.bloqueiaVisao = bloqueiaVisao;
		this.setFaccao("OBJETO"); // Facção neutra/especial

		// Garante que comece cheio
		this.setVidaAtual(this.getVidaMaxima());
	}

	private static Map<Atributo, Integer> criarAtributosFixos(int defesa) {
		Map<Atributo, Integer> atr = new HashMap<>();
		for (Atributo a : Atributo.values())
			atr.put(a, 1);
		atr.put(Atributo.TOPOR, defesa); // Topor serve como dureza/RD
		return atr;
	}

	// Objetos nunca têm turno ativo
	@Override
	public boolean isAtivoNoCombate() {
		return false; // Nunca aparece na lista de turnos
	}

	public boolean isIntacto() {
		return this.getVidaAtual() > 0;
	}

	public boolean isBloqueiaVisao() {
		return bloqueiaVisao;
	}
}