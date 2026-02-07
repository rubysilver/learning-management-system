create table if not exists refresh_tokens (
    id bigserial primary key,
    user_id bigint not null,
    token_hash varchar(100) not null,
    expires_at timestamptz not null,
    constraint uk_refresh_tokens_token_hash unique (token_hash),
    constraint fk_refresh_tokens_user foreign key (user_id) references users(id) on delete cascade
);

create index if not exists idx_refresh_tokens_user_id on refresh_tokens(user_id);

