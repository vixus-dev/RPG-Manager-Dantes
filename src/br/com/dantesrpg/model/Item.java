package br.com.dantesrpg.model;

public abstract class Item {
	private String nome;
	private String descricao;
	private int valorMoedas;
	private String tipoMoeda = "BRONZE";
	private boolean usavelEmCombate;
	private int grauOverclock = 0;

	public static final int OVERCLOCK_MAXIMO = 10;
	public static final double OVERCLOCK_BONUS_POR_GRAU = 0.25;

	public Item(String nome, String descricao, int valorMoedas, boolean usavelEmCombate) {
		this.nome = nome;
		this.descricao = descricao;
		this.valorMoedas = valorMoedas;
		this.usavelEmCombate = usavelEmCombate;
	}

	// --- Overclock ---
	public int getGrauOverclock() {
		return grauOverclock;
	}

	public void setGrauOverclock(int grau) {
		this.grauOverclock = Math.max(0, Math.min(grau, OVERCLOCK_MAXIMO));
	}

	public double getMultiplicadorOverclock() {
		return 1.0 + OVERCLOCK_BONUS_POR_GRAU * grauOverclock;
	}

	public String getNomeComOverclock() {
		if (grauOverclock <= 0) return nome;
		String faiscas;
		if (grauOverclock <= 3) {
			faiscas = "\u26A1";
		} else if (grauOverclock <= 6) {
			faiscas = "\u26A1\u26A1";
		} else if (grauOverclock <= 9) {
			faiscas = "\u26A1\u26A1\u26A1";
		} else {
			faiscas = "\u2726\u26A1\u26A1\u26A1";
		}
		return nome + " " + faiscas + "\u00D7" + grauOverclock;
	}

	public boolean isOverclockado() {
		return grauOverclock > 0;
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

	protected java.util.List<String> habilidadesConcedidasNomes = new java.util.ArrayList<>();

	public java.util.List<String> getHabilidadesConcedidasNomes() {
		return habilidadesConcedidasNomes;
	}

	public void addHabilidadeConcedida(String nome) {
		if (nome != null && !nome.isEmpty()) {
			this.habilidadesConcedidasNomes.add(nome);
		}
	}

}