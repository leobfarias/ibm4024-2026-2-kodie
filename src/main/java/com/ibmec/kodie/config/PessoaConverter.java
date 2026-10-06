package com.ibmec.kodie.config;

import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.repository.PessoaRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Converte o id enviado pelo <select> do formulario na entidade Pessoa.
 *
 * Sem este conversor, o Spring nao saberia transformar o texto "2" do campo
 * em um objeto Pessoa, e o vinculo da ocorrencia chegaria nulo.
 */
@Component
public class PessoaConverter implements Converter<String, Pessoa> {

    private final PessoaRepository repository;

    public PessoaConverter(PessoaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pessoa convert(@NonNull String id) {
        if (id.isBlank()) {
            return null;                       // ocorrencia sem pessoa vinculada
        }
        try {
            return repository.findById(Long.valueOf(id)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
