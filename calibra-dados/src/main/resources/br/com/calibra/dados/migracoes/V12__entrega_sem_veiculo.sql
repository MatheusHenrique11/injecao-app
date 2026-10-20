-- V12 (Fase 3B, bloco 3B.6): a entrega da OS em que o veículo não passou pela oficina (SPEC 6.5), como a de só a
-- confecção de chicote na bancada. Ela registra quem retirou, no lugar do km de saída. As OS que já existem ficam com o
-- campo vazio: foram entregues com o veículo.

ALTER TABLE ordem_servico ADD COLUMN retirado_por TEXT NOT NULL DEFAULT '';
