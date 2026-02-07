-- Simple initial schema for the LMS domain
-- Matches JPA entity mappings in com.krzelj.lms.domain.*

create table if not exists roles (
    id bigserial primary key,
    name varchar(32) not null,
    constraint uk_roles_name unique (name)
);

create table if not exists users (
    id bigserial primary key,
    username varchar(64) not null,
    password_hash varchar(255) not null,
    email varchar(255) not null,
    enabled boolean not null,
    locale varchar(8) not null,
    constraint uk_users_username unique (username),
    constraint uk_users_email unique (email)
);

create table if not exists user_roles (
    user_id bigint not null,
    role_id bigint not null,
    constraint pk_user_roles primary key (user_id, role_id),
    constraint uk_user_roles_user_role unique (user_id, role_id),
    constraint fk_user_roles_user foreign key (user_id) references users(id) on delete cascade,
    constraint fk_user_roles_role foreign key (role_id) references roles(id) on delete cascade
);

create table if not exists courses (
    id bigserial primary key,
    code varchar(32) not null,
    title varchar(200) not null,
    description varchar(4000) not null,
    instructor_id bigint not null,
    created_at timestamptz not null,
    constraint fk_courses_instructor foreign key (instructor_id) references users(id)
);

create table if not exists course_students (
    course_id bigint not null,
    student_id bigint not null,
    constraint pk_course_students primary key (course_id, student_id),
    constraint fk_course_students_course foreign key (course_id) references courses(id) on delete cascade,
    constraint fk_course_students_student foreign key (student_id) references users(id) on delete cascade
);

create table if not exists assignments (
    id bigserial primary key,
    course_id bigint not null,
    title varchar(200) not null,
    description varchar(4000) not null,
    due_at timestamptz not null,
    max_points integer not null,
    constraint fk_assignments_course foreign key (course_id) references courses(id) on delete cascade
);

create table if not exists submissions (
    id bigserial primary key,
    assignment_id bigint not null,
    student_id bigint not null,
    submitted_at timestamptz null,
    grade_points integer null,
    graded_at timestamptz null,
    graded_by_id bigint null,
    content_text varchar(10000) null,
    constraint uk_submissions_assignment_student unique (assignment_id, student_id),
    constraint fk_submissions_assignment foreign key (assignment_id) references assignments(id) on delete cascade,
    constraint fk_submissions_student foreign key (student_id) references users(id) on delete cascade,
    constraint fk_submissions_graded_by foreign key (graded_by_id) references users(id)
);

create index if not exists idx_courses_instructor_id on courses(instructor_id);
create index if not exists idx_assignments_course_id on assignments(course_id);
create index if not exists idx_submissions_assignment_id on submissions(assignment_id);
create index if not exists idx_submissions_student_id on submissions(student_id);

