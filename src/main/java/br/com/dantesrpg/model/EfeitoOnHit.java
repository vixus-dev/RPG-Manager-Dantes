package br.com.dantesrpg.model;

public class EfeitoOnHit {
	private String nome;
	private String efeito;
	private double chance;

	public EfeitoOnHit() {
	}

	public EfeitoOnHit(String nome, double chance) {
		this.nome = nome;
		this.chance = chance;
	}

	public String getNome() {
		if (nome != null && !nome.isBlank()) {
			return nome;
		}
		return efeito;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEfeito() {
		return getNome();
	}

	public void setEfeito(String efeito) {
		this.efeito = efeito;
	}

	public double getChance() {
		return chance;
	}

	public void setChance(double chance) {
		this.chance = chance;
	}

	public boolean isValido() {
		String nomeFinal = getNome();
		return nomeFinal != null && !nomeFinal.isBlank() && chance > 0.0;
	}
}
