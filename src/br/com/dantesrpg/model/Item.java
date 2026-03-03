package br.com.dantesrpg.model;

public abstract class Item {
	private String nome;
	private String descricao;
	private int valorMoedas;
	private String tipoMoeda = "BRONZE";
	private boolean usavelEmCombate;

	public Item(String nome, String descricao, int valorMoedas, boolean usavelEmCombate) {
		this.nome = nome;
		this.descricao = descricao;
		this.valorMoedas = valorMoedas;
		this.usavelEmCombate = usavelEmCombate;
	}

	// --- Getters ---
	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public int getValorMoedas() {
		return valorMoedas;
	}

	public boolean isUsavelEmCombate() {
		return usavelEmCombate;
	}

	public String getTipoMoeda() {
		return tipoMoeda;
	}

	public void setTipoMoeda(String tipoMoeda) {
		this.tipoMoeda = tipoMoeda;
	}

	public void usar(Personagem usuario, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		System.out.println(">>> " + this.getNome() + " não pode ser 'usado' dessa forma.");
	}

	public int getCustoTU() {
		return 0; // Padrão: Equipamentos não têm custo de TU para "usar"
	}

	public abstract String getTipo();

}