package me.infamous.accessmod.common.entity.ai;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.function.Predicate;

public class ConditionalGoal<T extends MobEntity, G extends Goal> extends Goal {

    private final Predicate<T> condition;
    private final T mob;
    private final G goal;
    private final boolean checkWhileUsing;

    public ConditionalGoal(Predicate<T> condition, T mob, G goal, boolean checkWhileUsing){
        this.condition = condition;
        this.mob = mob;
        this.goal = goal;
        this.checkWhileUsing = checkWhileUsing;
    }

    public ConditionalGoal(Predicate<T> condition, T mob, G goal){
        this(condition, mob, goal, false);
    }

    @Override
    public boolean canUse() {
        return this.condition.test(this.mob) && this.goal.canUse();
    }

    @Override
    public void start() {
        this.goal.start();
    }

    @Override
    public void tick() {
        this.goal.tick();
    }

    @Override
    public boolean canContinueToUse() {
        return (!this.checkWhileUsing || this.condition.test(this.mob)) && this.goal.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return this.goal.isInterruptable();
    }

    @Override
    public EnumSet<Flag> getFlags() {
        return this.goal.getFlags();
    }

    @Override
    public void setFlags(EnumSet<Flag> pFlagSet) {
        this.goal.setFlags(pFlagSet);
    }

    @Override
    public int hashCode() {
        return this.goal.hashCode();
    }

    @Override
    public String toString() {
        return "ConditionalGoal: " + this.goal;
    }
}
