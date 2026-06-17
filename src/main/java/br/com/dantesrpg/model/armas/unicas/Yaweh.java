package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.Grimorio;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.util.HabilidadeFactory;

public class Yaweh extends Grimorio {

	private boolean despertarDivinoAtivo = false;

	// Valores originais
	private final int alcanceOriginal = 3;

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

	/**
	 * Ativa o modo Despertar Divino: alcance 1, ataque básico melee,
	 * ataque alternativo em cone 100° com 1.2x de dano e alcance 2.
	 */
	public void ativarDespertarDivino() {
		this.despertarDivinoAtivo = true;
		this.setTipoAlvo(TipoAlvo.INDIVIDUAL);
		this.setTamanhoArea(0);
	}

	/**
	 * Desativa o modo Despertar Divino e restaura o estado original.
	 */
	public void desativarDespertarDivino() {
		this.despertarDivinoAtivo = false;
		this.setTipoAlvo(TipoAlvo.AREA_CIRCULAR);
		this.setTamanhoArea(1);
	}

	public boolean isDespertarDivinoAtivo() {
		return despertarDivinoAtivo;
	}

	// --- Overrides para ataque alternativo durante Despertar Divino ---

	@Override
	public int getAlcance() {
		if (despertarDivinoAtivo) {
			return 1;
		}
		return alcanceOriginal;
	}

	@Override
	public boolean hasAtaqueAlternativoBasico() {
		return despertarDivinoAtivo;
	}

	@Override
	public String getNomeAtaqueAlternativoBasico() {
		return "Varredura Divina";
	}

	@Override
	public String getDescricaoAtaqueAlternativoBasico() {
		return "Cone 100°, 1.2x Dano, Alcance 2";
	}

	@Override
	public double getMultiplicadorAtaqueAlternativoBasico() {
		return 1.20;
	}

	@Override
	public int getAlcanceAtaqueAlternativoBasico() {
		return 2;
	}

	@Override
	public TipoAlvo getTipoAlvoAtaqueAlternativoBasico() {
		return TipoAlvo.CONE;
	}

	@Override
	public int getAnguloAtaqueAlternativoBasico() {
		return 100;
	}

	@Override
	public int getTamanhoAreaAtaqueAlternativoBasico() {
		return 2;
	}
}