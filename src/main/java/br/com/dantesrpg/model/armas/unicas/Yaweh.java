package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.Grimorio;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.util.HabilidadeFactory;

public class Yaweh extends Grimorio {
	public Yaweh() {
		super("Yaweh", "Grimorio",
				"Biblia sagrada google insira imagens.png fml vai bolsonaro",
				Raridade.UNICO, 0, 20, Atributo.INSPIRACAO, 100, 3, 4);
		this.setTipoAlvo(br.com.dantesrpg.model.enums.TipoAlvo.AREA_CIRCULAR);
		this.setTamanhoArea(1);

		this.aprenderMagia(HabilidadeFactory.criarHabilidadePorNome("Benção Divina"));
		this.aprenderMagia(HabilidadeFactory.criarHabilidadePorNome("Proteção dos Céus"));
		this.aprenderMagia(HabilidadeFactory.criarHabilidadePorNome("Visão Divina"));
		this.aprenderMagia(HabilidadeFactory.criarHabilidadePorNome("Holy Spirit"));
	}
}