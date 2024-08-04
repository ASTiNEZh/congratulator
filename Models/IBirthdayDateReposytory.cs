using System.Collections.Generic;

namespace Congratulator.Models
{
    public interface IBirthdayDateReposytory
    {
        void Add(BirthdayDate item);
        IEnumerable<BirthdayDate> GetAll();
        BirthdayDate Find(long key);
        void Remove(long key);
        void Update(BirthdayDate item);
    }
}