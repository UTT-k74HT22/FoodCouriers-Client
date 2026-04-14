-- Social auth bootstrap requires an authenticated user to create
-- their own row in public.users the first time they login with Google.

create policy "users_self_insert"
on public.users
as permissive
for insert
to authenticated
with check (auth_id = auth.uid());
