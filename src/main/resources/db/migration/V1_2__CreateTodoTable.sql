CREATE TABLE todo (
  `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `uid` INT,
  `title` VARCHAR(128),
  `date` timestamp,
  `card_id` INT,
  constraint fk_todo_card_id foreign key(card_id) references card(`id`)
);

create unique index ix_todo_id on todo (`id`);