package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.racas.Lobisomem;

public class BastaoPrimordialBehemoth extends ArmaMelee {

	private Personagem portador;

	public BastaoPrimordialBehemoth() {
		super("Bastão Primordial Behemoth", "Bastão",
				"Um bastão ancestral imbuído com a essência de um Behemoth primordial. "
						+ "Quando seu portador assume a forma lupina, o bastão se transforma em um martelo colossal "
						+ "capaz de devastar uma área inteira com cada golpe.",
				Raridade.UNICO, 0, 10, 1, Atributo.FORCA, 100, 2);
	}

	private boolean isPortadorTransformado() {
		if (portador == null)
			return false;
		if (portador.getRaca() instanceof Lobisomem) {
			return portador.getRaca().isTransformed();
		}
		return false;
	}

	@Override
	public void onCombatStart(Personagem ator, br.com.dantesrpg.model.EstadoCombate estado) {
		this.portador = ator;
	}

	@Override
	public int getDanoBase() {
		return isPortadorTransformado() ? 15 : 10;
	}

	@Override
	public int getCustoTU() {
		return isPortadorTransformado() ? 115 : 100;
	}

	@Override
	public int getAlcance() {
		return isPortadorTransformado() ? 4 : 2;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return isPortadorTransformado() ? TipoAlvo.AREA_QUADRADA : TipoAlvo.INDIVIDUAL;
	}

	@Override
	public int getTamanhoArea() {
		return isPortadorTransformado() ? 3 : 0;
	}

	@Override
	public String getCategoria() {
		return isPortadorTransformado() ? "Martelo" : "Bastão";
	}

	@Override
	public String getDescricao() {
		if (isPortadorTransformado()) {
			return "O bastão se expandiu em um martelo colossal de proporções titânicas. "
					+ "Cada golpe causa devastação em uma área 3x3. Alcance 4, 15 de dano, 115 TU.";
		}
		return "Um bastão ancestral imbuído com a essência de um Behemoth primordial. "
				+ "Quando seu portador assume a forma lupina, o bastão se transforma em um martelo colossal "
				+ "capaz de devastar uma área inteira com cada golpe. Alcance 2, 10 de dano, 100 TU.";
	}

	// === Ataque Especial: Estender ===

	@Override
	public boolean hasAtaqueAlternativoBasico() {
		return true;
	}

	@Override
	public String getNomeAtaqueAlternativoBasico() {
		return "Estender";
	}

	@Override
	public String getDescricaoAtaqueAlternativoBasico() {
		return "1.3x Dano, 1.1x TU, +1 Mana, AOE Linha";
	}

	@Override
	public double getMultiplicadorAtaqueAlternativoBasico() {
		return 1.3;
	}

	@Override
	public int getAlcanceAtaqueAlternativoBasico() {
		return getAlcance(); // Mesmo alcance da forma atual
	}

	@Override
	public TipoAlvo getTipoAlvoAtaqueAlternativoBasico() {
		return TipoAlvo.LINHA;
	}

	@Override
	public int getTamanhoAreaAtaqueAlternativoBasico() {
		return 1;
	}

	@Override
	public double getCustoTUMultiplierAtaqueAlternativo() {
		return 1.1;
	}

	@Override
	public double getManaGainAtaqueAlternativo() {
		return 1.0;
	}
}
