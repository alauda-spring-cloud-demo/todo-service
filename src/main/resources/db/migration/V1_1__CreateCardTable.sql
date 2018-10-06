CREATE TABLE card (
  `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `project_id` INT,
  `title` VARCHAR(128)
);

create unique index ix_card_id on card (`id`);