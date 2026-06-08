package br.com.dantesrpg.model.util;

/**
 * Representa um único Contrato de Vida sobre um Personagem.
 *
 * Um contrato reduz o teto de HP máximo enquanto tiver dívida restante.
 * Múltiplas fontes podem coexistir e acumular (regras em {@link ContratoDeVidaUtils}).
 */
public class ContratoDeVida {

	public static final String FONTE_HUMANO = "Humano";
	public static final String FONTE_BARBARO = "Raiva Imparável";
	public static final String FONTE_RESPIRAR = "Respirar";
	public static final String FONTE_SIT_IN_BALANCE = "Sit in Balance";

	private final String fonte;
	private double valorTotal;
	private double dividaRestante;
	private int duracaoTURestante; // -1 = indefinido (expira só no fim do combate ou quando pago)
	private final boolean persisteAposCombate;

	// Bárbaro: stacks no momento da criação/atualização, usados para recalcular buff ao pagar.
	private int stacksBarbaroIniciais = 0;

	public ContratoDeVida(String fonte, double valorTotal, int duracaoTU, boolean persisteAposCombate) {
		this.fonte = fonte;
		this.valorTotal = valorTotal;
		this.dividaRestante = valorTotal;
		this.duracaoTURestante = duracaoTU;
		this.persisteAposCombate = persisteAposCombate;
	}

	public String getFonte() {
		return fonte;
	}

	public double getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(double valorTotal) {
		this.valorTotal = valorTotal;
	}

	public double getDividaRestante() {
		return dividaRestante;
	}

	public void setDividaRestante(double dividaRestante) {
		this.dividaRestante = Math.max(0, dividaRestante);
	}

	public int getDuracaoTURestante() {
		return duracaoTURestante;
	}

	public void setDuracaoTURestante(int duracaoTURestante) {
		this.duracaoTURestante = duracaoTURestante;
	}

	public boolean persisteAposCombate() {
		return persisteAposCombate;
	}

	public int getStacksBarbaroIniciais() {
		return stacksBarbaroIniciais;
	}

	public void setStacksBarbaroIniciais(int stacksBarbaroIniciais) {
		this.stacksBarbaroIniciais = stacksBarbaroIniciais;
	}

	public boolean isHumano() {
		return FONTE_HUMANO.equals(fonte);
	}

	public boolean isBarbaro() {
		return FONTE_BARBARO.equals(fonte);
	}
}
